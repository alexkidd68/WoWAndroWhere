-- The app's controller profile sends F1-F12 for the buttons and WASD for the left stick. Without ConsolePortLK
-- those would target party members and move the character, so in the world they're bound to a button that does nothing.
local KEYS = { "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "W", "A", "S", "D" }

local block = CreateFrame("Button", "AndroWhereControllerMenusBlock")
block:RegisterEvent("PLAYER_LOGIN")
block:SetScript("OnEvent", function(self)
    if IsAddOnLoaded("ConsolePort") then return end
    for _, key in ipairs(KEYS) do
        SetOverrideBindingClick(self, true, key, self:GetName())
    end
end)
