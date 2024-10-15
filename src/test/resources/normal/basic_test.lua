varA = 21

for i = 100, 50, (0-1)
do
    i = i + 1
    varA = varA + i
    for i = 100, 50, (0-1)
    do
        i = i + 1
        varA = varA + i
    end
end

return varA
