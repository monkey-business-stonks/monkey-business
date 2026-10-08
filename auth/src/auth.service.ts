import { Injectable, UnauthorizedException, ConflictException } from "@nestjs/common";
import { JwtService } from "@nestjs/jwt";

interface StoredUser {
  password: string;
  refreshToken: string | null;
}

@Injectable()
export class AuthService {
  // TODO: replace in-memory map with db access 
  private readonly users = new Map<string, StoredUser>([
    ["dave", { password: "mission123", refreshToken: null }],
  ]);

  constructor(private readonly jwtService: JwtService) {}

  register(username: string, password: string): { username: string; registered: true } {
    if (this.users.has(username)) {
      throw new ConflictException(`${username} is already registered`);
    }
    this.users.set(username, { password, refreshToken: null });
    return { username, registered: true };
  }

  login(username: string, password: string): { accessToken: string; refreshToken: string } {
    const user = this.users.get(username);
    if (!user || user.password !== password) {
      throw new UnauthorizedException("invalid username or password");
    }

    // Generate real JWTs with payloads
    const payload = { sub: username, username };
    
    const accessToken = this.jwtService.sign(payload, { expiresIn: '15m' });
    const refreshToken = this.jwtService.sign(payload, { expiresIn: '7d' });

    user.refreshToken = refreshToken;
    return { accessToken, refreshToken };
  }

  refresh(refreshToken: string): { accessToken: string } {
    try {
      // Cryptographically verify the incoming refresh token
      const payload = this.jwtService.verify(refreshToken);
      const user = this.users.get(payload.username);

      if (!user || user.refreshToken !== refreshToken) {
        throw new UnauthorizedException("invalid or expired refresh token");
      }

      // Issue a fresh access token
      const newPayload = { sub: payload.username, username: payload.username };
      const accessToken = this.jwtService.sign(newPayload, { expiresIn: '15m' });

      return { accessToken };
    } catch {
      throw new UnauthorizedException("invalid or expired refresh token");
    }
  }

  logout(refreshToken: string): { loggedOut: true } {
    const entry = this.findByRefreshToken(refreshToken);
    if (!entry) {
      throw new UnauthorizedException("invalid or expired refresh token");
    }
    const [, user] = entry;
    user.refreshToken = null;
    return { loggedOut: true };
  }

  private findByRefreshToken(refreshToken: string): [string, StoredUser] | undefined {
    const entries = Array.from(this.users.entries());
    for (const entry of entries) {
      const [, user] = entry;
      if (user.refreshToken === refreshToken) {
        return entry;
      }
    }
    return undefined;
  }
}