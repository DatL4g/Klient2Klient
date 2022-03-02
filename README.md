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

- [Discovery](#discovery)
  - [Make discoverable](#make-discoverable-can-be-found-by-other-hosts)
  - [Start discovery](#start-discovery-find-other-hosts)
  - [Get the found hosts](#get-the-found-hosts)

## Discovery

The discovery relies on a builder

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
val discover = Discovery.Builder(coroutneScope /* optional */)
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

