import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@/app/core/services/auth.service';

export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      return router.parseUrl('/login');
    }

    const role = authService.getUserRole();
    if (role && allowedRoles.includes(role)) {
      return true;
    }

    return router.parseUrl(authService.getDefaultRouteForRole());
  };
};

export const standardUserGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.parseUrl('/login');
  }

  const role = authService.getUserRole();
  if (role === 'ANALYST' || role === 'OPERATIONS') {
    return router.parseUrl(authService.getDefaultRouteForRole());
  }

  return true;
};