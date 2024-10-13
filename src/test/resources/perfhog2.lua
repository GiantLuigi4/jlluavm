-- this file designed for testing loop elimination
-- I do not know how long it takes to run this

varE = 2 + 3
varE = varE + 6

for i = 5, 10, 1 do
    varE = varE + 1
end

varE = 2.5

for i = 5, 10, 1 do
    varE = varE + 1
    for i = 5, 10, 1 do
        varE = varE + 1
        for i = 5, 10, 1 do
            varE = varE + 1
            for i = 5, 10, 1 do
                varE = varE + 1
                for i = 5, 10, 1 do
                    varE = varE + 1
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                end
                for i = 5, 10, 1 do
                    varE = varE + 1
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                end
            end
            for i = 5, 10, 1 do
                varE = varE + 1
                for i = 5, 10, 1 do
                    varE = varE + 1
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                end
                for i = 5, 10, 1 do
                    varE = varE + 1
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                    for i = 5, 10, 1 do
                        varE = varE + 1
                        for i = 5, 10, 1 do
                            varE = varE + 1
                            for i = 5, 10, 1 do
                                varE = varE + 1
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                            end
                            for i = 5, 10, 1 do
                                varE = varE + 1
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                            end
                        end
                        for i = 5, 10, 1 do
                            varE = varE + 1
                            for i = 5, 10, 1 do
                                varE = varE + 1
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                            end
                            for i = 5, 10, 1 do
                                varE = varE + 1
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                    for i = 5, 10, 1 do
                                        varE = varE + 1
                                        for i = 5, 10, 1 do
                                            varE = varE + 1
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                        end
                                        for i = 5, 10, 1 do
                                            varE = varE + 1
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                        end
                                    end
                                    for i = 5, 10, 1 do
                                        varE = varE + 1
                                        for i = 5, 10, 1 do
                                            varE = varE + 1
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                        end
                                        for i = 5, 10, 1 do
                                            varE = varE + 1
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                                for i = 5, 10, 1 do
                                                    varE = varE + 1
                                                    for i = 5, 10, 1 do
                                                        varE = varE + 1
                                                        for i = 5, 10, 1 do
                                                            varE = varE + 1
                                                        end
                                                        for i = 5, 10, 1 do
                                                            varE = varE + 1
                                                        end
                                                    end
                                                    for i = 5, 10, 1 do
                                                        varE = varE + 1
                                                        for i = 5, 10, 1 do
                                                            varE = varE + 1
                                                        end
                                                        for i = 5, 10, 1 do
                                                            varE = varE + 1
                                                        end
                                                    end
                                                end
                                                for i = 5, 10, 1 do
                                                    varE = varE + 1
                                                    for i = 5, 10, 1 do
                                                        varE = varE + 1
                                                        for i = 5, 10, 1 do
                                                            varE = varE + 1
                                                        end
                                                        for i = 5, 10, 1 do
                                                            varE = varE + 1
                                                        end
                                                    end
                                                    for i = 5, 10, 1 do
                                                        varE = varE + 1
                                                        for i = 5, 10, 1 do
                                                            varE = varE + 1
                                                        end
                                                        for i = 5, 10, 1 do
                                                            varE = varE + 1
                                                        end
                                                    end
                                                end
                                            end
                                        end
                                    end
                                end
                            end
                        end
                    end
                end
            end
        end
        for i = 5, 10, 1 do
            varE = varE + 1
        end
    end
    for i = 5, 10, 1 do
        varE = varE + 1
        for i = 5, 10, 1 do
            varE = varE + 1
        end
        for i = 5, 10, 1 do
            varE = varE + 1
        end
    end
end
for i = 5, 10, 1 do
    varE = varE + 1
    for i = 5, 10, 1 do
        varE = varE + 1
        for i = 5, 10, 1 do
            varE = varE + 1
        end
        for i = 5, 10, 1 do
            varE = varE + 1
        end
    end
    for i = 5, 10, 1 do
        varE = varE + 1
        for i = 5, 10, 1 do
            varE = varE + 1
        end
        for i = 5, 10, 1 do
            varE = varE + 1
            for i = 5, 10, 1 do
                varE = varE + 1
                for i = 5, 10, 1 do
                    varE = varE + 1
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                end
                for i = 5, 10, 1 do
                    varE = varE + 1
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                end
            end
            for i = 5, 10, 1 do
                varE = varE + 1
                for i = 5, 10, 1 do
                    varE = varE + 1
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                end
                for i = 5, 10, 1 do
                    varE = varE + 1
                    for i = 5, 10, 1 do
                        varE = varE + 1
                    end
                    for i = 5, 10, 1 do
                        varE = varE + 1
                        for i = 5, 10, 1 do
                            varE = varE + 1
                            for i = 5, 10, 1 do
                                varE = varE + 1
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                            end
                            for i = 5, 10, 1 do
                                varE = varE + 1
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                            end
                        end
                        for i = 5, 10, 1 do
                            varE = varE + 1
                            for i = 5, 10, 1 do
                                varE = varE + 1
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                            end
                            for i = 5, 10, 1 do
                                varE = varE + 1
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                end
                                for i = 5, 10, 1 do
                                    varE = varE + 1
                                    for i = 5, 10, 1 do
                                        varE = varE + 1
                                        for i = 5, 10, 1 do
                                            varE = varE + 1
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                        end
                                        for i = 5, 10, 1 do
                                            varE = varE + 1
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                        end
                                    end
                                    for i = 5, 10, 1 do
                                        varE = varE + 1
                                        for i = 5, 10, 1 do
                                            varE = varE + 1
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                        end
                                        for i = 5, 10, 1 do
                                            varE = varE + 1
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                            for i = 5, 10, 1 do
                                                varE = varE + 1
                                            end
                                        end
                                    end
                                end
                            end
                        end
                    end
                end
            end
        end
    end
end

return varE