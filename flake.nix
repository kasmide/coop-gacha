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
          scala sbt jdk nodejs
        ];
      };
      apps.${system}.generate-menu = {
        type = "app";
        program = "${pkgs.writeShellScriptBin "generate-menu" ''
          ${pkgs.nushell}/bin/nu ./tools/retrieve_menu.nu
        ''}/bin/generate-menu";
      };
    };
}