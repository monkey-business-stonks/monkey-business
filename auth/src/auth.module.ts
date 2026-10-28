import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';

const jwtSigningKey = process.env.JWT_SIGNING_KEY;
if (!jwtSigningKey?.trim()) {
  throw new Error('JWT_SIGNING_KEY must be set');
}

@Module({
  imports: [
    JwtModule.register({
      secret: jwtSigningKey,
      signOptions: { 
        algorithm: 'HS256', 
        expiresIn: '15m'
      },
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService],
})
export class AuthModule {}