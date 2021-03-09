import {Injectable} from '@angular/core'
import {IvanaChessService} from './ivana-chess.service'
import {HttpClient, HttpErrorResponse} from '@angular/common/http'
import {BehaviorSubject, Observable, of, throwError} from 'rxjs'
import {RxStompService} from '@stomp/ng2-stompjs'
import {User} from './user'
import {catchError, mergeMap, tap} from 'rxjs/operators'
import {Credentials} from './credentials'
import {ApiError} from './api-error'
import {ApiErrorCode} from './api-error-code.enum'

/**
 * Authentication service.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthenticationService extends IvanaChessService {
  /**
   * Path.
   * @private
   */
  private path: string = '/auth'

  /**
   * True if it is first call to this service, false otherwise.
   * @private
   */
  private firstCall: boolean = true

  /**
   * Subject which contains authenticated user.
   * @private
   */
  private user = new BehaviorSubject<User | null>(null)

  /**
   * Initialize service.
   *
   * @param http HTTP client.
   * @param stompService Stomp service.
   */
  constructor(
    http: HttpClient,
    stompService: RxStompService
  ) {
    super(http, stompService)
  }

  /**
   * Generate JWT for user.
   *
   * @param creds User credentials.
   * @return Observable which contains user or null if log-in failed.
   */
  logIn(creds: Credentials): Observable<User | null> {
    return this.post(this.path, creds)
      .pipe(
        mergeMap(() => this.fetchMe()),
        mergeMap(() => this.user)
      )
  }

  /**
   * Log-out.
   *
   * @return Empty observable.
   */
  logOut(): Observable<void> {
    return this.delete(this.path)
      .pipe(tap(() => this.user.next(null)))
  }

  /**
   * Get current authenticated user.
   *
   * @return Observable which contains authenticated user or null if user is anonymous.
   */
  me(): Observable<User | null> {
    if (!this.firstCall) {
      return this.user
    } else {
      return this.fetchMe()
        .pipe(mergeMap(() => this.user))
    }
  }

  /**
   * Fetch authenticated user.
   *
   * @return Observable which contains authenticated user or null if user is anonymous.
   * @private
   */
  private fetchMe(): Observable<User | null> {
    return this.get<User>(this.path)
      .pipe(
        catchError((errorResponse: HttpErrorResponse) => {
          const error = (errorResponse.error as ApiError)
          if (error.code === ApiErrorCode.Unauthorized) {
            return of(null)
          } else {
            return throwError(errorResponse)
          }
        }),
        tap(user => {
          this.user.next(user)
          this.firstCall = false
        })
      )
  }
}
