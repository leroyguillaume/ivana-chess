import {Injectable} from '@angular/core'
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree} from '@angular/router'
import {Observable} from 'rxjs'
import {AuthenticationService} from './authentication.service'
import {map} from 'rxjs/operators'

/**
 * Guard which ensure user is anonymous.
 */
@Injectable({
  providedIn: 'root'
})
export class AnonymousGuard implements CanActivate {
  /**
   * Initialize guard.
   *
   * @param authService Authentication service.
   * @param router Router
   */
  constructor(
    private authService: AuthenticationService,
    private router: Router
  ) {
  }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    return this.authService.me()
      .pipe(map(user => user === null))
  }
}
