import {Component, OnInit} from '@angular/core'
import {AbstractControl, FormControl, FormGroup, Validators} from '@angular/forms'
import {AuthenticationService} from '../authentication.service'
import {Router} from '@angular/router'
import {catchError, finalize} from 'rxjs/operators'
import {ErrorService} from '../error.service'

/**
 * Log-in component.
 */
@Component({
  selector: 'app-login',
  templateUrl: './log-in.component.html',
  styleUrls: ['./log-in.component.scss']
})
export class LogInComponent implements OnInit {
  /**
   * Log-in form.
   */
  logInForm = new FormGroup({
    pseudo: new FormControl('', Validators.required),
    password: new FormControl('', Validators.required)
  })

  /**
   * True if log-in is pending, false otherwise.
   */
  logInPending: boolean = false

  /**
   * Initialize component.
   *
   * @param authService Authentication service.
   * @param errorService Error service.
   * @param router Router.
   */
  constructor(
    private authService: AuthenticationService,
    private errorService: ErrorService,
    private router: Router
  ) {
  }

  /**
   * Get password form control.
   *
   * @return AbstractControl Form control.
   */
  get password(): AbstractControl {
    return this.logInForm.get('password')!!
  }

  /**
   * Get pseudo form control.
   *
   * @return AbstractControl Form control.
   */
  get pseudo(): AbstractControl {
    return this.logInForm.get('pseudo')!!
  }

  /**
   * Submit log-in form.
   */
  logIn(): void {
    this.logInPending = true
    this.authService.logIn(this.pseudo.value, this.password.value)
      .pipe(
        catchError(error => this.errorService.handleApiError(error)),
        finalize(() => this.logInPending = false)
      )
      .subscribe(() => this.router.navigate(['/']))
  }

  ngOnInit(): void {
  }

}
