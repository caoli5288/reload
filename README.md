# Reload
A Bukkit plugin that customize server's restart behavior. Public under GPLv2.

## Command
- /uptime
  - This command print "2017-12-07 up 33 min(s), 39920 tick(s); Load avg: 20.0, 20.0, 20.0".
- /at
  - This command schedule command to run at today or tomorrow's given clock.
  - Syntax like "/at 12:00 say hi", "/at 2018-11-11T00:01 say hi" or "/at +1h say hi".
  - Allowed unit suffix are (s)econd, (m)inus, (h)our and (d)ay.
- /every
  - This command schedule command to run at everyday(s)'s given clock.
  - Syntax like "/every 12:00 say hello"
  - or "/every 1h say hello". Allowed unit suffix are (s)econd, (m)inus, (h)our and (d)ay.
- /atq
  - This command manage all scheduled task.
- /shutdown
  - Kick all player(s) to other hub defined in `config.yml` and stop the server.
- /halt
  - Kill the server immediately without data save and cleanup.
  - This command call `taskkill /f` if server in windows, other wise `kill -9`.
- /sudo
  - The "Switch user do" just like linux's.
  - Syntax like "/sudo him say hahaha".
- /dumpmemory
  - dump JVM.
- /async <commands>
  - Call commands asynchronous.
- /echo <player_name> \[msg...]
  - Format messages for given player. 
- /exit
  - Shutdown server if no players online.
- /lag [time]
  - Start or stop server lag simulation.
  - Optional parameter to set lag time in milliseconds per tick.
- /msg <player_name> <message...>
  - Send formatted message to specified player.
  - Support color codes with &.
- /curl \<url> \[data]
- /alias \<command>
  - Detect if the command is an alias
- /rconnect \<player> \<server>
  - Transport the player to another server.
- /velocity \<player> \[multiplier]
  - Give a push to the player. You can specify the multiplier.
- /cmdall \<command...>
  - Execute commands for all players. Each player's placeholders will be replaced.
- /cmd <player_name> <command...>
  - Execute commands for given player.
  - Use `@r` to select random one.
  - Use `@<int>` to select random players. 
  - placeholders will be replaced.
- /if \<...>
  - See below.

## Expression
Run expression in config.yml to known when to shutdown. If you want to disable it, set it to null or empty string. 

### Expression variable
| variable | description                 |
|----------|-----------------------------|
| flow     | Count player join and quit  |
| join     | Count player joined         |
| login    | Count player try login      |
| memory   | Ratio of memory usage(0-1)  |
| online   | Current online-player count |
| time     | Server run time by second   |
| tps      | Latest 1 minute tps(0-20)   |

## Commands alt checker

Enable commands alt checker by setting `commands_alt_checker` in config.yml. You must set the name of the command instead of the alias, and all aliases will be resolved automatically.

```yaml
commands_alt_checker:
  # deny if player not has permission "command.help.use"
  - "help"
  - "plugins"
  - "version"
```

## Citizens traits

### Terms

Spawn and remove npc automatic.

```yaml
npc:
  '0':
    traits:
      terms:
        # spawn time(inclusive, optional)
        from: 2021-12-25T00:00
        # remove time(exclusive, optional)
        to: 2022-01-01T00:00
```

### Holograms

Holograms with refresh time. 

- FASTEST - 0.1 seconds.
- FAST - 0.5 seconds
- MEDIUM - 1 seconds
- SLOW - 5 seconds
- SLOWEST - 10 seconds.

```yaml
npc:
  '0':
    traits:
      holograms:
        # BOTTOM_UP or TOP_DOWN, default to TOP_DOWN
        direction: TOP_DOWN
        # default to -1
        lineHeight: -1.0
        lines:
          '0': 第一行
          '1': 第二行
          '2': '%time_until_2021-12-24T00:00%'
        # default to SLOW
        refresh: SLOW
```

### Commands

```yaml
npc:
  '0':
    traits:
      commands:
        # Set to -1 to disable, default to 500ms.
        cd: 500
        # CONSOLE or PLAYER, default to CONSOLE
        mode: CONSOLE
        # Run anyway
        commands:
          '0': say hello, %player_name%
        rules:
          # Run if matches rules
          '0':
            if: '%player_has_permission_vip%'
            cmd: |
              give %player_name% stone
              give %player_name% diamond_pickaxe
            # Break pipeline if matched
            continuous: false
          '1':
            cmd: title %player_name% actionbar You are not VIP
```

### Hide

```yaml
npc:
  '0':
    traits:
      hide:
        rules:
          '0':
            # Show NPC if matches rule, or check next if exists.
            if: '%player_has_permission_<permissionName>%'
            hide: false
          '1':
            # Hide NPC if matches rule, or show if rules end.
            if: '!%player_has_permission_<permissionName2>%'
            hide: true
```

## Traffic rules

You can combine rules.

```yaml
traffic_rules:
  # Log all activity
  - action: LOG

  # Dump stack trace if context is "Server thread"
  - action: TRACE
    context: Server thread

  # Accept if host is "trusted.com"
  - action: ACCEPT
    host: trusted.com

  # Reject if host is "example.com" and context is "Server thread"
  - action: REJECT
    context: Server thread
    scheme: http(s)?
    host: example.com
```

## Placeholders

- `%uptime_time%`
- `%uptime_ticks%`
- `%uptime_tps%`
- `%uptime_tps1%`
- `%uptime_tps2%`
- `%uptime_tps3%`
- `%datetime_now%`
- `%datetime_now_yyyyMMddHHmmss%`
- `%datetime_until_2023-01-01T00:00%`
- `%nextint_100%` - Returns a random integer between 0 and 100
- `%nextint_1_100%` - Returns a random integer between 1 and 100
- `%nexttext_a_b_c%` - Returns a random text from the given list
- `%nexttext_@file.txt%` - Returns a random line from the specified file

## If command examples

### Simple expr condition

- `if expr %vip_level% > 0; then give %player_name% diamond; tell %player_name% You got diamond!; else give %player_name% apple; tell %player_name% You got apple!; fi`

### Commands condition

Command must correctly implement the return value, i.e., return false on failure.

- `if coins take %player_name% 100; then give %player_name% diamond; else tell %player_name% You don't have enough coins!; fi`

### With elif

- `if expr %vip_level% == 3; then give %player_name% diamond 3; elif expr %vip_level% == 2; then give %player_name% diamond 2; else give %player_name% diamond; fi`

### With other commands

An example of give money to all vip's every 2h.

`every 2h cmdall if expr %vip_level% > 0; then money give %player_name% 50; fi`
