{ pkgs }: {
    deps = [
        pkgs.imagemagick6_light
        pkgs.graalvm17-ce
        pkgs.maven
        pkgs.replitPackages.jdt-language-server
        pkgs.replitPackages.java-debug
    ];
}