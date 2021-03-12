import {Component, OnInit} from '@angular/core'
import {AbstractControl, FormControl, FormGroup, ValidationErrors, Validators} from '@angular/forms'
import {UserService} from '../user.service'
import {UserSubscription} from '../user-subscription'
import {Router} from '@angular/router'
import {ApiErrorCode} from '../api-error-code.enum'
import {catchError, debounceTime, finalize, switchMap, tap} from 'rxjs/operators'
import {ErrorService} from '../error.service'

/**
 * Sign-up component.
 */
@Component({
  selector: 'app-sign-up',
  templateUrl: './sign-up.component.html',
  styleUrls: ['./sign-up.component.scss']
})
export class SignUpComponent implements OnInit {
  /**
   * Pseudo minimal length.
   */
  readonly pseudoMinLength = 3

  /**
   * Pseudo maximal length.
   */
  readonly pseudoMaxLength = 50

  /**
   * Password minimal length.
   */
  readonly passwordMinLength = 5

  /**
   * Sign-up form.
   */
  signUpForm = new FormGroup(
    {
      pseudo: new FormControl('', [
        Validators.required,
        Validators.minLength(this.pseudoMinLength),
        Validators.maxLength(this.pseudoMaxLength),
        Validators.pattern(/^[A-z0-9_-]+$/)
      ]),
      email: new FormControl('', [Validators.required, Validators.email]),
      password: new FormControl('', [Validators.required, Validators.minLength(this.passwordMinLength)]),
      passwordConfirmation: new FormControl('', [Validators.required])
    },
    [SignUpComponent.checkPasswords]
  )

  /**
   * API error code.
   */
  errorCode: ApiErrorCode | null = null

  /**
   * True if sign-up is pending, false otherwise.
   */
  signUpPending: boolean = false

  /**
   * True if pseudo is checking, false otherwise.
   */
  checkingPseudo: boolean = false

  /**
   * True if pseudo is already used, false if it is not, null if it is uncheck.
   */
  pseudoExists: boolean | null = null

  /**
   * True if email is checking, false otherwise.
   */
  checkingEmail: boolean = false

  /**
   * True if email is already used, false if it is not, null if it is uncheck.
   */
  emailExists: boolean | null = null

  /**
   * Check if password confirmation is same as password.
   *
   * @param control Form.
   * @return Validation errors if password confirmation is not same as password, null otherwise.
   * @private
   */
  private static checkPasswords(control: AbstractControl): ValidationErrors | null {
    const valid = control.get('password')?.value === control.get('passwordConfirmation')?.value
    return valid ? null : {passwordConfirmation: 'invalid'}
  }

  /**
   * Initialize component.
   *
   * @param userService User service.
   * @param errorService Error service.
   * @param router Router.
   */
  constructor(
    private userService: UserService,
    private errorService: ErrorService,
    private router: Router
  ) {
  }

  /**
   * Get email form control.
   *
   * @return Form control.
   */
  get email(): AbstractControl {
    return this.signUpForm.get('email')!!
  }

  /**
   * Get password form control.
   *
   * @return Form control.
   */
  get password(): AbstractControl {
    return this.signUpForm.get('password')!!
  }

  /**
   * Get password confirmation form control.
   *
   * @return Form control.
   */
  get passwordConfirmation(): AbstractControl {
    return this.signUpForm.get('passwordConfirmation')!!
  }

  /**
   * Get pseudo form control.
   *
   * @return Form control.
   */
  get pseudo(): AbstractControl {
    return this.signUpForm.get('pseudo')!!
  }

  ngOnInit(): void {
    this.pseudo.valueChanges
      .pipe(
        debounceTime(300),
        tap(() => {
          this.checkingPseudo = true
          this.pseudoExists = null
        }),
        switchMap(pseudo =>
          this.userService.existsByPseudo(pseudo)
            .pipe(finalize(() => this.checkingPseudo = false))
        )
      )
      .subscribe(exists => this.pseudoExists = exists)
    this.email.valueChanges
      .pipe(
        debounceTime(300),
        tap(() => {
          this.checkingEmail = true
          this.emailExists = null
        }),
        switchMap(email =>
          this.userService.existsByEmail(email)
            .pipe(finalize(() => this.checkingEmail = false))
        )
      )
      .subscribe(exists => this.emailExists = exists)
  }

  /**
   * Submit sign-up form.
   */
  signUp(): void {
    this.signUpPending = true
    const userCreation = this.signUpForm.value as UserSubscription
    this.userService.signUp(userCreation)
      .pipe(
        catchError(error => this.errorService.handleApiError(error)),
        finalize(() => this.signUpPending = false)
      )
      .subscribe(() => this.router.navigate(['/login']).then())
  }
}
