import { Injectable, UnauthorizedException, ConflictException } from "@nestjs/common";
import { JwtService } from "@nestjs/jwt";
import * as bcrypt from 'bcryptjs';

interface StoredUser {
  passwordHash: string;
  refreshToken: string | null;
}

@Injectable()
export class AuthService {
  private readonly SALT_ROUNDS = 10;

  // TODO: replace in-memory map with db access 
  private readonly users = new Map<string, StoredUser>([
    [
      "dave",
      {
        passwordHash: "$2b$10$Ep39f4XNfQp/8C6H/Qd7le0h9Gv7x1S5V8C9v4G/8d7le0h9Gv7x1", // Example hash for "mission123"
        refreshToken: null,
      },
    ],
  ]);

  constructor(private readonly jwtService: JwtService) {}

  async register(username: string, password: string): Promise<{ username: string; registered: true }> {
    if (this.users.has(username)) {
      throw new ConflictException(`${username} is already registered`);
    }

    const passwordHash = await bcrypt.hash(password, this.SALT_ROUNDS);
    this.users.set(username, { passwordHash, refreshToken: null });

    return { username, registered: true };
  }

  async login(username: string, password: string): Promise<{ accessToken: string; refreshToken: string }> {
    const user = this.users.get(username);
    
    // Check user existence and compare plain text password against stored hash
    if (!user || !(await bcrypt.compare(password, user.passwordHash))) {
      throw new UnauthorizedException("invalid username or password");
    }

    const payload = { sub: username, username };

    const accessToken = this.jwtService.sign(payload, { expiresIn: "15m" });
    const refreshToken = this.jwtService.sign(payload, { expiresIn: "7d" });

    user.refreshToken = refreshToken;
    return { accessToken, refreshToken };
  }

  refresh(refreshToken: string): { accessToken: string } {
    try {
      const payload = this.jwtService.verify(refreshToken);
      const user = this.users.get(payload.username);

      if (!user || user.refreshToken !== refreshToken) {
        throw new UnauthorizedException("invalid or expired refresh token");
      }

      const newPayload = { sub: payload.username, username: payload.username };
      const accessToken = this.jwtService.sign(newPayload, { expiresIn: "15m" });

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