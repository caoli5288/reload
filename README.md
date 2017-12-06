# Reload
A Bukkit plugin that customize server's restart behavior. Public under GPLv2.

## Variable
| variable | description |
|----------|-------------|
| time     | Server run time by second   |
| online   | Current onlin -player count |
| flow     | Count total player joined   |
| memory   | Ratio of memory usage(0-1)  |
| tps      | Latest 1 minute tps(0-20)   |

## Command
- /at
  - This command schedule command to run at today or tomorrow's given clock.
  - Syntax like "/at 12:00 say hi", "/at 2018-11-11T00:01 say hi" or "/at +1h say hi".
  - Allowed unit suffix are (s)econd, (m)inus, (h)our and (d)ay.
- /every
  - This command schedule command to run at everyday(s)'s given clock.
  - Syntax like "/every 12:00 say hello".
