
resolvers += Resolver.mavenCentral

resolvers += Classpaths.typesafeReleases
resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("staging")
resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.jcenterRepo

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.8.0")

addSbtPlugin("com.github.mwz" % "sbt-sonar" % "2.2.0")
