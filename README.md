# Klient2Klient

### Kotlin Multiplatform

This is a Kotlin Multiplatform project, however it is implemented and works ONLY on JVM right now.
This means any JVM project/platform can use this library, so Android and Java Desktop/Server applications can run it.

[![Issues](https://img.shields.io/github/issues/DATL4G/Klient2Klient.svg?style=for-the-badge)](https://github.com/DATL4G/Klient2Klient/issues)
[![Stars](https://img.shields.io/github/stars/DATL4G/Klient2Klient.svg?style=for-the-badge)](https://github.com/DATL4G/Klient2Klient)
[![Forks](https://img.shields.io/github/forks/DATL4G/Klient2Klient.svg?style=for-the-badge)](https://github.com/DATL4G/Klient2Klient/network/members)
[![Contributors](https://img.shields.io/github/contributors/DATL4G/Klient2Klient.svg?style=for-the-badge)](https://github.com/DATL4G/Klient2Klient/graphs/contributors)
[![License](https://img.shields.io/github/license/DATL4G/Klient2Klient.svg?style=for-the-badge)](https://github.com/DATL4G/Klient2Klient/blob/master/LICENSE)

![Kotlin](https://img.shields.io/badge/kotlin-%230095D5.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)

### Discover and create Peer to Peer connections easily

- Discover hosts in the same network
- Connect and transfer data among clients

### Table of contents

- [Usage](#usage)
- [Discovery](#discovery)
  - [Make discoverable](#make-discoverable-can-be-found-by-other-hosts)
  - [Start discovery](#start-discovery-find-other-hosts)
  - [Get the found hosts](#get-the-found-hosts)
- [Connection](#connection)
  - [Start/Stop Receiving](#startstop-receiving)
  - [Send/Collect data](#sendcollect-data)
- [Contributing](#contributing)
  - [Maintainers](#maintainers)
- [Support the project](#support-the-project)

## Usage

Add the JitPack repository to your build file

```gradle
allprojects {
  repositories {
    ...
    maven { url = uri("https://jitpack.io") }
  }
}
```

Add the dependency

```gradle
implementation("com.github.DatL4g:Klient2Klient:$latestVersion")
```

## Discovery

The discovery relies on a builder and provides the following options

- ```setDiscoveryTimeout(milliSecondsOrDuration)``` after which time it stops discovering (searching for other hosts)
- ```setDiscoveryTimeoutListener{ }``` called in IO dispatcher but is a suspend function, so you can switch context easily
- ```setDiscoverableTimeout(milliSecondsOrDuration)``` after which time it stops broadcasting to other hosts (cannot be discovered anymore)
- ```setDiscoverableTimeoutListener{ }``` called in IO dispatcher but is a suspend function, so you can switch context easily
- ```setPing(milliSecondsOrDuration)``` at which interval it broadcasts to other hosts
- ```setPuffer(milliSecondsOrDuration)``` tolerance in which time the hosts need to ping again (since it's UDP, packages may be lost or not sent)
- ```setPort(int)``` on which port to broadcast and listen
- ```setHostFilter(regex)``` the discovered hosts need to match this (keep empty (and filterMatch in Host) for any or change the regex to ".*")
- ```setScope(coroutineScope)``` change the scope in which the discovery jobs are running in
- ```setHostIsClient(boolean)``` whether the current host should be discovered ass a client too (default off)
- ```build()``` create an instance of Discovery

The builder can be called using two different methods.

```kotlin
val discover = Discovery.Builder(coroutineScope /* optional */)
  .setPort(1337)
  .otherBuilderMethods()
  .build()
```

```kotlin
val discover = coroutineScope.discovery {
  setPort(1337)
  otherBuilderMethods()
}
```

### Make discoverable (can be found by other hosts)

Basically the host holds three parameter

- name: the name of the host
- filterMatch: the String to match the hostFilter regex
- optionalInfo: any JsonElement

Call any of the following methods:

```kotlin
discover.makeDiscoverable(Host("name", "filterMatch" /* optional */, jsonElement /* optional */))
```

```kotlin
discover.makeDiscoverable("name", "filterMatch" /* optional */, jsonElement /* optional */)
```

```kotlin
discover.makeDiscoverable("name", "filterMatch" /* optional */, "{jsonString: true}" /* optional */)
```

To stop being discoverable imply call

```kotlin
discover.stopBeingDiscoverable()
```

### Start discovery (find other hosts)

```kotlin
discover.startDiscovery()
//or
discover.startDiscovery(hostIsClient)
```

To stop discovery simply call

```kotlin
discover.stopDiscovery()
```

### Get the found hosts

To get the current list of found hosts synchronously use:

```kotlin
val setOfHosts = discover.peers
```

The host list can be collected to always get the latest items

```kotlin
discover.peersFlow.collect { hosts ->
  // got new hosts
}
```

## Connection

The connection relies on a builder as well with these options:

- ```fromDiscovery(discover)``` just to pass the peers
- ```forPeers(setOfHosts)``` pass the peers yourself
- ```forPeer(host)``` only connect to one host
- ```setPort(int)``` the port on which the connection will be established
- ```setScope(coroutineScope)``` change the scope in which the connection jobs are running in
- ```build()``` create an instance of Connection

The builder can be used two different ways again:

```kotlin
val connect = Connection.Builder(coroutineScope /* optional */)
  .fromDiscovery(discover)
  .setPort(7331)
  .build()
```

```kotlin
val connect = coroutineScope.connection {
  fromDiscovery(discover)
  setPort(7331)
}
```

### Start/Stop Receiving

To start receiving just call, this will allow to get data from another host

```kotlin
  connect.startReceiving()
```

Stopping is as easy as starting

```kotlin
  connect.stopReceiving()
```

### Send/Collect data

To send data to all connected hosts use

```kotlin
  connect.send(bytes)
```

To send data to one host only use

```kotlin
  connect.send(bytes, host)
```

Collecting data can be done using Flows again

```kotlin
  connect.receiveData.collect { pair: Pair<Host, ByteArray> ->
    // received data from another host
  }
```

## Contributing

When you face any bugs or problems please open an [Issue](https://github.com/DATL4G/Klient2Klient/issues/new/choose).

To add functionality fork the project and create a pull request afterwards. You should know how that works if you are a developer :)
You can add yourself to the list below if you want then.

### Maintainers

| Avatar | Contributor |
|---|:---:|
| [![](https://avatars3.githubusercontent.com/u/46448715?s=50&v=4)](http://github.com/DatL4g) | [DatLag](http://github.com/DatL4g) |

## Support the project

[![Github-sponsors](https://img.shields.io/badge/sponsor-30363D?style=for-the-badge&logo=GitHub-Sponsors&logoColor=#EA4AAA)](https://github.com/sponsors/DATL4G)
[![PayPal](https://img.shields.io/badge/PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white)](https://paypal.me/datlag)
[![Patreon](https://img.shields.io/badge/Patreon-F96854?style=for-the-badge&logo=patreon&logoColor=white)](https://www.patreon.com/datlag)

Supporting this project helps to keep it up-to-date. You can donate if you want or contribute to the project as well.
This shows that the library is used by people and it's worth to maintain.
