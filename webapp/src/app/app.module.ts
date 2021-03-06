import {NgModule} from '@angular/core'
import {BrowserModule} from '@angular/platform-browser'

import {AppRoutingModule} from './app-routing.module'
import {AppComponent} from './app.component'
import {HomeComponent} from './home/home.component'
import {HttpClientModule} from '@angular/common/http'
import {GameComponent} from './game/game.component'
import {InjectableRxStompConfig, RxStompService, rxStompServiceFactory} from '@stomp/ng2-stompjs'
import {StompConfig} from './stomp-configuration'
import {LogInComponent} from './log-in/log-in.component'
import {ReactiveFormsModule} from '@angular/forms'
import {AuthenticationService} from './authentication.service'
import {SignUpComponent} from './sign-up/sign-up.component'
import {NgbModule} from '@ng-bootstrap/ng-bootstrap'
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome'
import {NavbarComponent} from './navbar/navbar.component'
import {NewGameComponent} from './new-game/new-game.component'
import {MessageComponent} from './message/message.component'
import {ErrorPageComponent} from './error-page/error-page.component'
import {MatchmakingComponent} from './matchmaking/matchmaking.component'
import {ProfileComponent} from './profile/profile.component'
import {ConfirmModalComponent} from './confirm-modal/confirm-modal.component'

/**
 * Ivana Chess module.
 */
@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    GameComponent,
    LogInComponent,
    SignUpComponent,
    NavbarComponent,
    NewGameComponent,
    MessageComponent,
    ErrorPageComponent,
    MatchmakingComponent,
    ProfileComponent,
    ConfirmModalComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    ReactiveFormsModule,
    NgbModule,
    FontAwesomeModule
  ],
  providers: [
    {
      provide: InjectableRxStompConfig,
      useValue: StompConfig
    },
    {
      provide: RxStompService,
      useFactory: rxStompServiceFactory,
      deps: [InjectableRxStompConfig]
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  /**
   * Initialize module.
   *
   * @param authService Authentication service.
   */
  constructor(
    private authService: AuthenticationService,
  ) {
  }
}
