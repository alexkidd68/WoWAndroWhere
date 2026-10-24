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
