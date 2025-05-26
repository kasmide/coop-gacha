{
  description = "build env";

  inputs = { nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable"; };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
    in {
      devShells.${system}.default = pkgs.mkShell {
        buildInputs = with pkgs; [
          scala sbt nodejs
        ];
      };
      apps.${system}.ci-generate-menu = {
        type = "app";
        program = "${pkgs.writeShellScriptBin "generate-menu" ''
          set -e
          ${pkgs.curl}/bin/curl -L "https://gitlab.com/api/v4/projects/$CI_PROJECT_ID/jobs/artifacts/$CI_COMMIT_BRANCH/download?job=generate-menu" | ${pkgs.libarchive}/bin/bsdtar -x
          ${pkgs.nushell}/bin/nu ./tools/retrieve_menu.nu
        ''}/bin/generate-menu";
      };
    };
}