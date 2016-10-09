# canaveral

Draft of a Java service manager.

## Running

Run the program with the name of the file containing as the first argument and then execute commands by writing to STDIN.

### Messages

* `RESUME-ALL`: starts/resumes execution of all services.
* `STOP-ALL`: requests all services to stop running.
* `RESUME-SERVICE a`: starts/resumes execution of a specific service.
* `STOP-SERVICE a`: requests a specific service to stop running.
* `EXIT`: requests all services to stop running and, after that has happened, terminates de `ServiceManager`.

### File Format

Each line of the file represents the dependency list for the first service in the line:

```
d b c
b a
c a
e
```
