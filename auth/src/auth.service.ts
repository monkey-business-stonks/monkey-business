import { Injectable, UnauthorizedException } from "@nestjs/common";
import { JwtService } from "@nestjs/jwt";

@Injectable()
export class AuthService {
  constructor(private readonly jwtService: JwtService) {}

  async login(username: string): Promise<{ accessToken: string; refreshToken: string }> {
    const payload = { sub: username, username };

    const accessToken = this.jwtService.sign(payload, { expiresIn: "15m" });
    const refreshToken = this.jwtService.sign(payload, { expiresIn: "7d" });

    return { accessToken, refreshToken };
  }

  refresh(refreshToken: string): { accessToken: string } {
    try {
      const payload = this.jwtService.verify(refreshToken);

      const newPayload = { sub: payload.username, username: payload.username };
      const accessToken = this.jwtService.sign(newPayload, { expiresIn: "15m" });

      return { accessToken };
    } catch {
      throw new UnauthorizedException("invalid or expired refresh token");
    }
  }
}