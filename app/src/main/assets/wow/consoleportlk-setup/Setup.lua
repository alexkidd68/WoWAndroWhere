-- Loads before ConsolePort ("!" sorts first) and fills in its settings when ConsolePort's saved
-- variables arrive, before ConsolePort reads them. Runs again only when PRESET changes.
local PRESET = 1

local LAYOUT = {
	CP_M1 = 'CP_TL2', CP_M2 = 'CP_TR2',
	CP_T1 = 'CP_TL1', CP_T2 = 'CP_TR1',
	CP_T3 = 'CP_L_GRIP1', CP_T4 = 'CP_R_GRIP1',
	CP_T5 = 'CP_L_GRIP2', CP_T6 = 'CP_R_GRIP2',
}

-- Must match assets/wow/consoleportlk.icp.
local KEYS = {
	CP_R_DOWN = 'F1', CP_R_RIGHT = 'F2', CP_R_LEFT = 'F3', CP_R_UP = 'F4',
	CP_L_UP = 'F5', CP_L_DOWN = 'F6', CP_L_LEFT = 'F7', CP_L_RIGHT = 'F8',
	CP_T1 = 'F9', CP_T2 = 'F10', CP_X_LEFT = 'F11', CP_X_RIGHT = 'F12',
}

local frame = CreateFrame('Frame')
frame:RegisterEvent('ADDON_LOADED')
frame:SetScript('OnEvent', function(self, _, name)
	if name ~= 'ConsolePort' then return end
	self:UnregisterEvent('ADDON_LOADED')
	local settings = ConsolePortSettings
	if settings and settings.androWherePreset == PRESET then return end
	if not settings then
		settings = ConsolePort:GetDefaultAddonSettings()
		ConsolePortSettings = settings
	end
	settings.type = 'XBOX'
	for key, value in pairs(LAYOUT) do settings[key] = value end
	settings.calibration = {}
	for button, key in pairs(KEYS) do settings.calibration[button] = key end
	settings.skipGuideBtn = true
	settings.skipCP_T3, settings.skipCP_T4, settings.skipCP_T5, settings.skipCP_T6 = true, true, true, true
	settings.stickRadialType = 2
	settings.stickRadialLocal = false
	settings.newController = true
	settings.androWherePreset = PRESET
end)

-- Immersion-WotLK leaves its ConsolePort support out of its toc (Logic\Controller.lua), so while it's open the face
-- buttons would still jump and attack. While its window takes the keyboard, the pad drives it here.
local function WireImmersion()
	local npc, handle = ImmersionFrame, ConsolePortUIHandle
	if not (npc and npc.SetPropagateKeyboardInput and npc.TitleButtons and ImmersionAPI and handle) then return end
	local KEY = ConsolePort:GetData().KEY
	local titles = npc.TitleButtons
	local input = CreateFrame('Button', 'AndroWhereImmersionInput')
	local focus, choice = 1, 0

	local function Hint(key, text)
		if text then handle:AddHint(KEY[key], text) else handle:RemoveHint(KEY[key]) end
	end

	local function Options()
		local list = {}
		if titles:IsShown() then
			for i = 1, #titles.Buttons do
				local button = titles.Active[i]
				if button and button:IsShown() then list[#list + 1] = button end
			end
		end
		return list
	end

	local function Choices()
		local list = {}
		local rewards = npc.TalkBox.Elements.Content.RewardsFrame
		if npc.hasItems and rewards and rewards.Buttons then
			for _, item in ipairs(rewards.Buttons) do
				if item:IsVisible() and item.type == 'choice' then list[#list + 1] = item end
			end
		end
		return list
	end

	local function Focus(delta)
		local options = Options()
		for _, button in ipairs(options) do
			button:UnlockHighlight()
			button.Hilite:SetAlpha(0.01)
		end
		if #options == 0 then return end
		focus = math.max(1, math.min(#options, focus + delta))
		options[focus]:LockHighlight()
		options[focus].Hilite:SetAlpha(1)
		Hint('CROSS', ACCEPT)
	end

	local function Accept()
		local options = Options()
		local text = npc.TalkBox.TextFrame.Text
		if options[focus] then
			options[focus]:Click()
		elseif text:IsSequence() and text:GetNumRemaining() > 1 then
			text:ForceNext()
		elseif npc.lastEvent == 'GOSSIP_SHOW' then
			ImmersionAPI:CloseGossip()
		else
			npc.TalkBox:OnLeftClick()
		end
	end

	local function Choose(delta)
		local choices = Choices()
		if #choices == 0 then return end
		choice = (choice + delta - 1) % #choices + 1
		choices[choice]:Click()
	end

	local function Close()
		ImmersionAPI:CloseGossip()
		ImmersionAPI:CloseQuest()
		ImmersionAPI:CloseItemText()
	end

	local actions = {
		CP_R_DOWN = Accept,
		CP_R_RIGHT = Close, CP_X_LEFT = Close, CP_X_RIGHT = Close,
		CP_R_LEFT = function() npc.TalkBox.TextFrame.Text:RepeatTexts() end,
		CP_L_UP = function() Focus(-1) end, CP_L_DOWN = function() Focus(1) end,
		CP_L_LEFT = function() Choose(-1) end, CP_L_RIGHT = function() Choose(1) end,
	}
	local actionFor = {}
	for button, action in pairs(actions) do actionFor[KEYS[button]] = action end
	input:SetScript('OnClick', function(_, key)
		local action = actionFor[key]
		if action then action() end
	end)

	local EVENT_HINTS = {
		QUEST_DETAIL = ACCEPT, QUEST_PROGRESS = CONTINUE, QUEST_COMPLETE = COMPLETE_QUEST,
	}

	hooksecurefunc(npc, 'SetPropagateKeyboardInput', function(self)
		if InCombatLockdown() then return end
		for key in pairs(actionFor) do SetOverrideBindingClick(input, true, key, input:GetName(), key) end
		focus, choice = 1, 0
		handle:SetHintFocus(self)
		handle.HintBar:Show()
		Hint('CIRCLE', CLOSE)
		Hint('CROSS', EVENT_HINTS[self.playbackEvent] or NEXT)
		Focus(0)
	end)

	hooksecurefunc(titles, 'UpdateActive', function()
		focus = 1
		if handle:IsHintFocus(npc) then Focus(0) end
	end)

	hooksecurefunc(npc, 'ClearKeyboardInput', function(self)
		if InCombatLockdown() then return end
		ClearOverrideBindings(input)
		for _, button in ipairs(Options()) do button:UnlockHighlight() end
		if handle:IsHintFocus(self) then
			handle:HideHintBar()
			handle:SetHintFocus(nil)
		end
		handle:ClearHintsForFrame(self)
	end)
end

local login = CreateFrame('Frame')
login:RegisterEvent('PLAYER_LOGIN')
login:SetScript('OnEvent', WireImmersion)
