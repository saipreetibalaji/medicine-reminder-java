# medicine-reminder-java
JavaFX-based Medicine Reminder Application with profile management, scheduling, and notification system
# Medicine Reminder App (using Java)

## Overview

This project is a JavaFX-based desktop application designed to help users manage their medication schedules efficiently. It allows users to create profiles, add medicines, track dosages, and receive timely reminders.

---

## Features

* Multi-profile support for different users
* Add, view, and delete medicines
* Scheduled reminders (morning, afternoon, night)
* Dashboard with medication summary
* Missed dose tracking system
* Light/Dark theme toggle
* Data persistence using file storage
* Upcoming reminders and daily schedule view

---

## Key Concepts Used

* Object-Oriented Programming (OOP)
* JavaFX UI development
* Event handling
* File handling (serialization & storage)
* Multithreading (ScheduledExecutorService for reminders)

---

## Tech Stack

* Java
* JavaFX
* Eclipse IDE

---

## Project Structure

```
medicine-reminder-javafx/
 ├── mini_project/
 │     ├── MedicineReminderFX.java
```

## How It Works

* Users create profiles to manage medications
* Medicines are scheduled with specific timings
* A background scheduler checks for reminders every minute
* Alerts are displayed when it’s time to take medicine
* Missed doses are automatically tracked

---

## Data Handling

* Data is stored locally using file serialization
* Readable data export is also generated

---

## Future Improvements

* Add sound notifications
* Mobile app integration
* Database integration (instead of file storage)
* Enhanced UI/UX

