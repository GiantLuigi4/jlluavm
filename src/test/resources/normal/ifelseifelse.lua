varA = 20
varB = 20

if varA > varB
then
    varB = varA - 1
elseif varA == varB
then
    varA = varA + 1
    varB = 0
else
    varA = varB - 1
end

return varA - varB