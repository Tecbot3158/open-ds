# The FRC Driver Station Protocol (2022)

This write-up attempts to document the reverse-engineered Protocol
utilized by [open-ds](https://github.com/Tecbot3158/open-ds), an open
source driver station designed to be used on devices **other** than
MS Windows 10 computers.

---

First things first. Let's try to build this on our own.
I extracted this code from a github workflow in the repo:

```
mvn -B package --file pom.xml
```

but that doesn't actually build it afaik.

So let's try something else, maybe looking @ the pom.xml file?
Not much in there besides the package name and a **main class!**.

Great, now we know where our java program starts:
`<mainClass>com.boomaa.opends.display.DisplayEndpoint</mainClass>`
which translates to `src/main/java/com/boomaa/opends/display/DisplayEndpoint`
relative to the root dir of the repository.

The file contains a lot of stuff. Includes a ton of libraries (local, I guess)
and creates a lot of objects. 

Also implements an interface and overrides the method `onCycle` in multiple
objects.

I don't really understand much of the `public static void main` code, it just does a lot 
of initialization and kidna runs on a loop. I should maybe try debugging this with
some jetbrains IDE or something.


But I got another idea, why not start from the top of the class definition and work our way through?
Ok, so there's a version tag string, meh, then there's an object initialization from the
`DSLog` class, then an instance of a NTConnection, then some UDP and TCP object interfaces (not
java interfaces) are defined, also some `FMS` objects which I think stands for
*Field Management System*.

Also, an InitChecker object is created and instantiated, then some 

Protocol yearsconstants.


Then we might be arriving @ something, on line 42 we can find several `ProtocolClass` objects which
take as a parameter the name of some classes:
- `Parser`
- `Creator`
- `Updater`

Also, a Clock class is declared with name rioTcp and I think runs every 20 ms (milliseconds)
