# Reload
A Bukkit plugin that customize server's restart behavior. Public under GPLv2.

## Command
- /at
  - This command schedule command to run at today or tomorrow's given clock.
  - Syntax like "/at 12:00 say hi", "/at 2018-11-11T00:01 say hi" or "/at +1h say hi".
  - Allowed unit suffix are (s)econd, (m)inus, (h)our and (d)ay.
- /every
  - This command schedule command to run at everyday(s)'s given clock.
  - Syntax like "/every 12:00 say hello".

## Expression
Bukkit run expression in config.yml to known when to shutdown. If you want to disable it, set it to null or empty string.

### Variable
| variable | description |
|----------|-------------|
| flow     | Count player join and quit  |
| join     | Count player joined         |
| login    | Count player try login      |
| memory   | Ratio of memory usage(0-1)  |
| online   | Current online-player count |
| time     | Server run time by second   |
| tps      | Latest 1 minute tps(0-20)   |

