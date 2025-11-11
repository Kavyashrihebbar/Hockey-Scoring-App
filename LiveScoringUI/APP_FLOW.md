# Live Scoring App - Flow Documentation

## Application Structure

### 1. MainActivity
- **Purpose**: Entry point that launches LiveScoringActivity
- **Flow**: Automatically redirects to Live Scoring screen

### 2. LiveScoringActivity (Main Screen)
**Features:**
- Display team scores (Team A vs Team B)
- Show current period and timer
- Goal and Saves buttons for both teams
- Control buttons for penalties and substitutions
- Goal log showing player actions
- Bottom navigation bar

**Interactive Elements:**
- **Timer Display**: Click to open Timer screen
- **+1 Goal Buttons**: Increment team scores
- **Saves Buttons**: Track saves (placeholder)
- **Red Flag Button**: Opens Major Penalty modal
- **Yellow Flag Button**: Opens Minor Penalty modal
- **Blue Refresh Button**: Opens Player Substitution modal

### 3. TimerActivity
**Features:**
- Countdown timer (starts at 15:00)
- Circular progress indicator
- Play/Pause button
- Stop/Reset button

**Controls:**
- **Play Button**: Starts countdown
- **Pause Button**: Pauses countdown
- **Stop Button**: Resets to 15:00
- **Back Arrow**: Returns to Live Scoring

### 4. Modal Dialogs

#### Minor Penalty Modal (Yellow Flag)
- Select offending team
- Select player
- Choose penalty type (Tripping, Slashing, etc.)
- Duration: 2:00
- Actions: Cancel or Add Penalty

#### Major Penalty Modal (Red Flag)
- Same structure as Minor Penalty
- Different header title
- Duration: 2:00
- Actions: Cancel or Add Penalty

#### Player Substitution Modal (Blue Refresh)
- Select team
- Choose player coming in
- Choose player going out
- Actions: Cancel or Confirm

## User Flow

```
MainActivity
    ↓
LiveScoringActivity (Main Screen)
    ├→ Click Timer → TimerActivity
    ├→ Click Red Flag → Major Penalty Modal
    ├→ Click Yellow Flag → Minor Penalty Modal
    ├→ Click Blue Refresh → Substitution Modal
    └→ Click +1 Goal → Update Score
```

## Key Features

1. **Simple Navigation**: All screens accessible from main screen
2. **Modal Overlays**: Smooth animations for penalty/substitution forms
3. **Timer Integration**: Separate countdown timer for quarter breaks
4. **Score Tracking**: Real-time score updates
5. **Goal Log**: Visual history of game events

## Technical Notes

- **Language**: Java
- **UI**: XML layouts with Material Design components
- **Animations**: Custom slide-up and fade-in effects
- **State Management**: Simple activity-based state
- **No Database**: All data in memory (suitable for college project)
