# HealthMate

HealthMate is an Android health-tracking app. Users can log their daily steps, calories and water intake, and review their progress over time.

Built for CI6330 Mobile Application Development.

## Features
- Register and log in (passwords hashed with PBKDF2, "Remember me" session)
- Dashboard with daily summary
- Add, update and delete daily health records (steps, calories, water, note)
- History screen with search, sorting and a 7-day filter
- Trends bar chart showing progress against the daily step goal
- Input validation for steps, calories and water

## Tech Stack
- Java, Android SDK (minimum SDK 26)
- SQLite (SQLiteOpenHelper)
- MVC structure: model, view, controller, data, ui

## Project Structure
```
app/src/main/java/com/healthmate/app/
  controller/   AuthController, HealthController, HistoryController
  data/         LocalDatabase
  model/        User, HealthRecord
  ui/           Ui, TrendChartView
  view/         MainActivity
```

## How to Run
1. Clone the repository: `git clone https://github.com/Janakan-SG/HealthMate-Android.git`
2. Open the folder in Android Studio
3. Wait for Gradle sync to finish
4. Run the app on an emulator or device (Android 8.0 or higher)

## Demo Account (test only)
- Email: janakan@gmail.com
- Password: janakan123

This account and its sample data are created automatically for testing only.

## Version Control
- `main`: stable, working version of the app
- `feature/data-layer`: models and SQLite database
- `feature/controllers`: authentication, health record and history controllers
- `feature/ui`: UI components and MainActivity
- `feature/tests`: test classes
- `docs/readme`: project documentation
- `docs/screenshots`: app screenshots