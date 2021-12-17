# Changelog

Note that new releases update the API types and services and may change or remove classes, methods, and types without
notice.

## [Unreleased]

## [0.3.4] - 2021-12-17

### Changed
* Add deprecation annotations to types, properties, and methods. Deprecations may be removed in future changes to the 
  API.

### Changed
* New service definitions.

## [0.3.3] - 2021-09-15

### Changed
* Updated services and types.
* Updated dependencies.

## [0.3.2] - 2021-01-20

### Added
* Added support for Bearer Authentication Token Support.

```java
import com.softlayer.api.*;
ApiClient client = new RestApiClient().withBearerToken("qqqqwwwweeeaaassddd....");
```

### Changed
* Updated services and types.

## [0.3.1] - 2020-11-09

### Changed
* Updated services and types.

## [0.3.0] - 2020-03-25

### Added
* Added a new `RestApiClient.BASE_SERVICE_URL` constant to use the client with the classic infrastructure private
  network.

### Changed
* A breaking change has been made. Coerce return types to whatever the API metadata says should be returned, even if
  the type returned by the API does not match (#64).

* Updated services and types.

## [0.2.9] - 2020-01-21

### Changed
* Updated generated services and types.
