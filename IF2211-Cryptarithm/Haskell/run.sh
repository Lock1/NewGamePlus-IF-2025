# Run parallel
cabal new-run Haskell -- par 16 +RTS -N16

# Run serial
# cabal run -O2