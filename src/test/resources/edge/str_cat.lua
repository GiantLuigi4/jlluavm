local mt = debug.getmetatable("a")
debug.setmetatable("a", {
    __index = mt.__index,
    __add = function(...)
        text = select(1, ...)
        text = text .. select(2, ...)
        return text
    end
});

print("Hello" + " " + " World" + "!")
