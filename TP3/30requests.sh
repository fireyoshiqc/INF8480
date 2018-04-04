#!/bin/bash
for i in {1..30}
do
    wget -O ./tp3get/index$i.html $1 &
done
wait