import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';

@Module({
  imports: [
    JwtModule.registerAsync({
      useFactory: () => {
        const signingKey = process.env.JWT_SIGNING_KEY;
        if (!signingKey) {
          throw new Error('JWT_SIGNING_KEY is required');
        }
        return {
          secret: signingKey,
          signOptions: {
            algorithm: 'HS256',
            expiresIn: '15m'
          },
        };
      },
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService],
})
export class AuthModule {}