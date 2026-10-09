// auth.controller.ts
import { Controller, Post, Body, HttpCode, HttpStatus, UnauthorizedException } from '@nestjs/common';
import { AuthService } from './auth.service';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @HttpCode(HttpStatus.OK)
  @Post('login')
  login(@Body() body: { userId: string; username: string; role: string }) {
    if (!body.userId || !body.username) {
      throw new UnauthorizedException('userId and username are required');
    }
    return this.authService.login(body.userId, body.username, body.role || 'USER');
  }

  @HttpCode(HttpStatus.OK)
  @Post('refresh')
  refresh(@Body() body: { refreshToken: string; username?: string }) {
    return this.authService.refresh(body.refreshToken, body.username);
  }

  @HttpCode(HttpStatus.OK)
  @Post('revoke')
  revoke(@Body() body: { token: string; username: string }) {
    if (!body.token || !body.username) {
      throw new UnauthorizedException('token and username are required');
    }
    this.authService.revokeToken(body.token, body.username);
    return { message: 'token revoked successfully' };
  }

  @HttpCode(HttpStatus.OK)
  @Post('revoke-all')
  revokeAll(@Body() body: { username: string }) {
    if (!body.username) {
      throw new UnauthorizedException('username is required');
    }
    const revokedCount = this.authService.revokeAllUserTokens(body.username);
    return { 
      message: `revoked all tokens for user ${body.username}`,
      revokedCount 
    };
  }
}