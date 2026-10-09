import { Injectable, UnauthorizedException } from "@nestjs/common";
import { JwtService } from "@nestjs/jwt";
import { v4 as uuidv4 } from 'uuid';

interface BlocklistEntry {
  jti: string;
  username: string;
  revokedAt: Date;
  expiresAt: Date;
}

@Injectable()
export class AuthService {

  private tokenBlocklist = new Map<string, BlocklistEntry>();
  private userTokens = new Map<string, Set<string>>();

  constructor(private readonly jwtService: JwtService) {
    setInterval(() => this.cleanupExpiredBlocklistEntries(), 5 * 60 * 1000);
  }

  async login(userId: string, username: string, role: string): Promise<{ accessToken: string; refreshToken: string }> {
    const jti = uuidv4();
    const payload = { 
      sub: userId, 
      username: username,
      role: role, 
      jti  
    };

    const accessToken = this.jwtService.sign(payload, { expiresIn: "15m" });
    const refreshToken = this.jwtService.sign(payload, { expiresIn: "7d" });

    if (!this.userTokens.has(username)) {
      this.userTokens.set(username, new Set());
    }
    this.userTokens.get(username)!.add(jti);

    return { accessToken, refreshToken };
  }

  refresh(refreshToken: string, username?: string): { accessToken: string } {
    try {
      const payload = this.jwtService.verify(refreshToken);
      
      if (this.isTokenBlocked(payload.jti)) {
        throw new UnauthorizedException("refresh token has been revoked");
      }

      if (username && payload.username !== username) {
        throw new UnauthorizedException("token does not belong to this user");
      }

      const newJti = uuidv4();
      const newPayload = { 
        sub: payload.sub,
        username: payload.username, 
        role: payload.role || 'USER', 
        jti: newJti
      };
      const accessToken = this.jwtService.sign(newPayload, { expiresIn: "15m" });

      if (!this.userTokens.has(payload.username)) {
        this.userTokens.set(payload.username, new Set());
      }
      this.userTokens.get(payload.username)!.add(newJti);

      return { accessToken };
    } catch (error) {
      throw new UnauthorizedException("invalid or expired refresh token");
    }
  }
  

  revokeToken(token: string, username: string): void {
    try {
      const payload = this.jwtService.verify(token);
      
      if (payload.username !== username) {
        throw new UnauthorizedException("cannot revoke token for another user");
      }

      this.addToBlocklist(payload.jti, username, payload.exp);
    } catch (error) {
      throw new UnauthorizedException("invalid token cannot be revoked");
    }
  }

  revokeAllUserTokens(username: string): number {
    const userTokenSet = this.userTokens.get(username);
    if (!userTokenSet) {
      return 0;
    }

    let revokedCount = 0;
    userTokenSet.forEach(jti => {
      this.tokenBlocklist.set(jti, {
        jti,
        username,
        revokedAt: new Date(),
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // Keep for max token lifetime
      });
      revokedCount++;
    });

    userTokenSet.clear();
    return revokedCount;
  }

  private isTokenBlocked(jti: string): boolean {
    return this.tokenBlocklist.has(jti);
  }

  private addToBlocklist(jti: string, username: string, expTimestamp: number): void {
    const expiresAt = new Date(expTimestamp * 1000);
    this.tokenBlocklist.set(jti, {
      jti,
      username,
      revokedAt: new Date(),
      expiresAt
    });

    const userTokenSet = this.userTokens.get(username);
    if (userTokenSet) {
      userTokenSet.delete(jti);
    }
  }

  /**
   * Cleanup expired entries from blocklist (runs periodically)
   */
  private cleanupExpiredBlocklistEntries(): void {
    const now = new Date();
    let cleanedCount = 0;
    
    this.tokenBlocklist.forEach((entry, jti) => {
      if (entry.expiresAt < now) {
        this.tokenBlocklist.delete(jti);
        cleanedCount++;
      }
    });

    if (cleanedCount > 0) {
      console.log(`[AuthService] Cleaned up ${cleanedCount} expired blocklist entries`);
    }
  }
}