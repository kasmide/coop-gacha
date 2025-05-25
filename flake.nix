{
  description = "build env";

  inputs = { nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable"; sbt.url = "github:zaninime/sbt-derivation"; };

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
    };
}