import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core'
import {Piece} from '../piece'

/**
 * Position component.
 */
@Component({
  selector: 'app-position',
  templateUrl: './position.component.html',
  styleUrls: ['./position.component.scss']
})
export class PositionComponent implements OnInit {
  /**
   * Piece.
   */
  @Input()
  piece: Piece | null = null

  /**
   * If this position is possible.
   */
  @Input()
  possible: boolean = false

  /**
   * Select event.
   */
  @Output()
  positionSelect: EventEmitter<Piece | null> = new EventEmitter<Piece | null>()

  /**
   * Choose event.
   */
  @Output()
  positionChoose: EventEmitter<void> = new EventEmitter<void>()

  constructor() {
  }

  ngOnInit(): void {
  }

  /**
   * Handle click event.
   *
   * Emit choose event if this position is possible, emit select event otherwise.
   */
  onClick(): void {
    if (this.possible) {
      this.positionChoose.emit()
    } else {
      this.positionSelect.emit(this.piece)
    }
  }
}