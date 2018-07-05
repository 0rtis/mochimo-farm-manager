#!/bin/bash

#update MINER_BIN_PATH with your path
MINER_BIN_PATH=$HOME"/mochimo/mochi/bin"


#move to bin folder
cd ${MINER_BIN_PATH}

#clear log file
echo '' > miner.log

#start miner and redirect outputs
./gomochi d -t0 > miner.log 2>&1 &
