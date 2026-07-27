/* BUY-01 learning header
 * File purpose: Bootstraps the standalone Angular application in the browser.
 * Learning focus: Angular application startup without NgModules.
 */
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
