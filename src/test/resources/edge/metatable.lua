local mt = debug.getmetatable("a")
debug.setmetatable("a", {
    __index = mt.__index,
    __call = function(...)
        text = "You have called the string value that represents the text that is seen as following: \""
        text = text .. ...
        text = text .. "\". Why have you done this?"
        print(text)

        text = "attempt to call"
        text = text .. type(select(1, ...))
        text = text .. "value"
        error(text)
    end
});

("A")()
