# DjVuLibre NuGet package

NuGet package is a zip file with .nuget extension, structured in a specific
way. You can use [NuGet Package Explorer][1] to view the contents.

The package can be created from the correct directory structure using the command
line tool [nuget][2]:

    nuget pack PATH\PACKAGE-NAME.nuspec

The tool will create the resulting .nuget package in the current directory.

You can publish the package in a local NuGet feed. First, create an empty directory
for the feed contents, say C:\local-nuget. Then run

    nuget add PACKAGE.nuget -Source C:\local-nuget

If you need to remove the installed package, run

    nuget delete PACKAGE VERSION -Source C:\local-nuget

Visual Studio can be connected to the created feed in the NuGet sources settings.
After connecting, you can add the NuGet package from the local feed to the project
using Visual Studio GUI as any other package. Just make sure that the correct feed
is selected.

The most important part of the djvulibre NuGet package configuration-wise
the is the .targets file. Note the Content element
that copies the DLL dependencies near the final executable.

This particular build of DjVuLibre is an officially unsupported 64-bit
configuration.

[1]: https://github.com/NuGetPackageExplorer/NuGetPackageExplorer
[2]: https://www.nuget.org/downloads