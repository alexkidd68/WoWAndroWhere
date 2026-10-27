-- AndroWhere Login: controller support on the 3.3.5a login screens.
-- The ConsolePortLK profile sends the pad as F-keys; the stock screens already understand
-- Enter, Escape and the arrows, so pad keys are translated and passed to Blizzard's own handlers.

local PAD = {
    F1 = "ENTER",   -- A
    F2 = "ESCAPE",  -- B
    F3 = "PAD_X",
    F4 = "PAD_Y",
    F5 = "UP",
    F6 = "DOWN",
    F7 = "LEFT",
    F8 = "RIGHT",
    F9 = "PAD_L1",
    F10 = "PAD_R1",
}

-- Ignores a repeat of the same key within 150 ms.
local lastKey, lastTime = nil, 0
local function translate(key)
    local mapped = PAD[key] or key
    local now = GetTime()
    if mapped == lastKey and now - lastTime < 0.15 then
        return nil
    end
    lastKey, lastTime = mapped, now
    -- Some stock handlers read the global arg1 instead of their key argument.
    arg1 = mapped
    return mapped
end

local function wrap(name, withSelf)
    local original = _G[name]
    if not original then return end
    if withSelf then
        _G[name] = function(self, key)
            local mapped = translate(key)
            if mapped then original(self, mapped) end
        end
    else
        _G[name] = function(key)
            local mapped = translate(key)
            if mapped then original(mapped) end
        end
    end
    return original
end

local function hint(parent, text)
    if not parent then return end
    local line = parent:CreateFontString(nil, "OVERLAY", "GlueFontNormalSmall")
    line:SetPoint("TOP", parent, "TOP", 0, -12)
    line:SetText(text)
    return line
end

-- Login with A: when the app passed the saved password (AndroWhereLoginData.lua, written only for one launch),
-- the password box is filled and left unfocused, so A reaches the login frame and logs in.
local password = AndroWhereLoginPassword
local autoLogin = AndroWhereLoginAuto
AndroWhereLoginPassword, AndroWhereLoginAuto = nil, nil

local function fillPassword()
    if password and AccountLoginPasswordEdit:GetText() == "" then
        AccountLoginPasswordEdit:SetText(password)
    end
end

-- Auto login presses Log in once per game start, half a second after the screen shows. After a failed
-- login or a disconnect it waits for A, so a wrong password isn't sent again and again.
local function tryAutoLogin()
    if not (password and autoLogin) then return end
    autoLogin = nil
    if AccountLoginAccountEdit:GetText() == "" or not TOSAccepted() then return end
    local waited = 0
    local timer = CreateFrame("Frame")
    timer:SetScript("OnUpdate", function(self, elapsed)
        waited = waited + (elapsed or arg1 or 0)
        if waited < 0.5 then return end
        self:SetScript("OnUpdate", nil)
        if AccountLogin:IsShown() and not GlueDialog:IsShown() and not TOSFrame:IsShown() then
            fillPassword()
            AccountLogin_Login()
        end
    end)
end

if password then
    local focusAccountName = AccountLogin_FocusAccountName
    AccountLogin_FocusPassword = function()
        fillPassword()
        AccountLoginPasswordEdit:ClearFocus()
        AccountLoginAccountEdit:ClearFocus()
        tryAutoLogin()
    end
    AccountLogin_FocusAccountName = function()
        fillPassword()
        focusAccountName()
    end
end

-- Without the password the box keeps the keyboard, so pad keys only reach this when it isn't focused.
-- B does nothing here, so the pad can't quit the game.
local accountLoginKey = AccountLogin_OnKeyDown
AccountLogin_OnKeyDown = function(key)
    local mapped = translate(key)
    if not mapped or mapped == "ESCAPE" and PAD[key] then return end
    if mapped == "ENTER" then fillPassword() end
    accountLoginKey(mapped)
end

local loginHint
if not password then
    loginHint = "Back, then Type password, to log in"
elseif GetSavedAccountName() ~= "" then
    loginHint = autoLogin and "Logging in. A  Log in again" or "A  Log in"
else
    loginHint = "Type your account name and press Enter. Tick Remember Account Name to log in with A next time."
end
hint(AccountLoginUI or AccountLogin, loginHint)

-- Character select: the D-pad picks, A enters the world, B goes back, Y changes realm.
local characterSelectKey = CharacterSelect_OnKeyDown
CharacterSelect_OnKeyDown = function(self, key)
    local mapped = translate(key)
    if not mapped then return end
    if mapped == "PAD_Y" then
        CharacterSelect_ChangeRealm()
    else
        characterSelectKey(self, mapped)
    end
end
hint(CharacterSelect, "D-pad  Choose       A  Enter world       B  Back       Y  Change realm")

-- Realm list: the D-pad moves through realms, L1 and R1 switch tabs.
local function selectRealm(step)
    local category = RealmList.selectedCategory or 1
    local count = GetNumRealms(category)
    if count == 0 then return end
    local current = RealmList.currentRealm or 0
    local index = current + step
    if index < 1 then index = count elseif index > count then index = 1 end
    RealmList.currentRealm = index
    RealmList.selectedName = GetRealmInfo(category, index)
    local offset = RealmList.offset or 0
    if index <= offset or index > offset + MAX_REALMS_DISPLAYED then
        local top = math.max(0, math.min(index - 1, count - MAX_REALMS_DISPLAYED))
        RealmListScrollFrameScrollBar:SetValue(top * REALM_BUTTON_HEIGHT)
    end
    RealmListUpdate()
end

local function switchRealmTab(step)
    local tabs = 0
    while _G["RealmListTab" .. (tabs + 1)] and _G["RealmListTab" .. (tabs + 1)]:IsShown() do
        tabs = tabs + 1
    end
    if tabs < 2 then return end
    local index = RealmList.selectedCategory or 1
    for _ = 1, tabs do
        index = index + step
        if index < 1 then index = tabs elseif index > tabs then index = 1 end
        local tab = _G["RealmListTab" .. index]
        if not tab.disabled then
            RealmListTab_OnClick(tab)
            return
        end
    end
end

local realmListKey = RealmList_OnKeyDown
RealmList_OnKeyDown = function(key)
    local mapped = translate(key)
    if not mapped then return end
    if mapped == "UP" or mapped == "LEFT" then
        selectRealm(-1)
    elseif mapped == "DOWN" or mapped == "RIGHT" then
        selectRealm(1)
    elseif mapped == "PAD_L1" then
        switchRealmTab(-1)
    elseif mapped == "PAD_R1" then
        switchRealmTab(1)
    else
        realmListKey(mapped)
    end
end
hint(RealmList, "D-pad  Choose       L1 R1  Tabs       A  Accept       B  Cancel")

-- Dialogs, the authenticator prompt, the account picker and cinematics: A and B act as Enter and Escape.
wrap("GlueDialog_OnKeyDown")
wrap("TokenEntryOkayButton_OnKeyDown", true)
wrap("WoWAccountSelect_OnKeyDown", true)
wrap("CinematicsFrame_OnKeyDown")
wrap("ChangedOptionsDialog_OnKeyDown", true)
