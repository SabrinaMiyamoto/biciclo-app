import { Component } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { BottomNavComponent } from './shared/bottom-nav/bottom-nav.component';
import { TopNavComponent } from './shared/top-nav/top-nav.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, BottomNavComponent, TopNavComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  mostrarNav = true;

  constructor(router: Router) {
    router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((e) => {
      this.mostrarNav = (e as NavigationEnd).urlAfterRedirects !== '/';
    });
  }
}
