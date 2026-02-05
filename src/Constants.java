/*
Copyright (c) 2025-2026 Arman Jussupgaliyev
*/
/** @noinspection UnnecessaryModifier*/
public interface Constants {
	
	// only primitive constants here, no objects or variables,
	//  so that proguard will omit this class
	
	static final int TPS = 30;
	static final int TIME_TICKS = (TPS * 42 / 60); // ticks per ingame second
	static final int ANIMATION_TICKS = 8; // ticks per animation cycle
	static final int PLAYER_SPEED = 4; // per tick
	static final int ANIMATE_SPEED = 2;
	static final int NPC_SPEED = 3;
	static final int OTHER_NPC_SPEED = 2;
	static final int FADE_SPEED = 6;
	
	static final int TILE_SIZE = 16;
	static final int NPC_VIEW_DISTANCE = 6 * 6 * TILE_SIZE * TILE_SIZE; // squared
	static final int NPC_CHASE_LOSE_DISTANCE = 12 * 12 * TILE_SIZE * TILE_SIZE; // squared
	static final int NPC_INTERACT_DISTANCE = 3 * TILE_SIZE * TILE_SIZE; // squared

	static final int SHADOW_COLOR = 0x50000000;

	static final boolean PROFILER = false;
	static final boolean LOGGING = true;
	static final boolean SERIAL_LOGS = false;
	static final boolean SCREEN_LOGS = false;
	
//#if FALSE
	// default config for development
	static final long FPS_LIMIT = 30;

	static final boolean BUFFER_SCREEN = false;
	static final boolean DRAW_SHADOWS = true;
	static final boolean DRAW_LIGHTS = true;
	static final boolean NOKIAUI_SHADOWS = false;
	static final boolean USE_M3G = true;
	static final boolean USE_TILED_LAYER = false; // more heap usage, but faster on s40, slower on s60
	static final boolean MORE_INMATES = true;

	static final boolean NO_SFX = false;
	static final boolean PREFETCH_MUSIC = false;
	static final boolean PREFETCH_SFX = false;
//#else
//#if FPS_LIMIT == ""
//#	static final long FPS_LIMIT = 30;
//#else
//#expand static final long FPS_LIMIT = %FPS_LIMIT%;
//#endif

//#if BUFFER_SCREEN == ""
//#	static final boolean BUFFER_SCREEN = false;
//#else
//#expand static final boolean BUFFER_SCREEN = %BUFFER_SCREEN%;
//#endif

//#if DRAW_SHADOWS == ""
//#	static final boolean DRAW_SHADOWS = true;
//#else
//#expand static final boolean DRAW_SHADOWS = %DRAW_SHADOWS%;
//#endif

//#if DRAW_LIGHTS == ""
//#	static final boolean DRAW_LIGHTS = true;
//#else
//#expand static final boolean DRAW_LIGHTS = %DRAW_LIGHTS%;
//#endif

//#if NOKIAUI_SHADOWS == ""
//#	static final boolean NOKIAUI_SHADOWS = false;
//#else
//#expand static final boolean NOKIAUI_SHADOWS = %NOKIAUI_SHADOWS%;
//#endif

//#if USE_M3G == ""
//#	static final boolean USE_M3G = true;
//#else
//#expand static final boolean USE_M3G = %USE_M3G%;
//#endif

//#if USE_TILED_LAYER == ""
//#	static final boolean USE_TILED_LAYER = true;
//#else
//#expand static final boolean USE_TILED_LAYER = %USE_TILED_LAYER%;
//#endif

//#if MORE_INMATES == ""
//#	static final boolean MORE_INMATES = true;
//#else
//#expand static final boolean MORE_INMATES = %MORE_INMATES%;
//#endif

//#if NO_SFX == ""
//#	static final boolean NO_SFX = false;
//#else
//#expand static final boolean NO_SFX = %NO_SFX%;
//#endif

//#if PREFETCH_MUSIC == ""
//#	static final boolean PREFETCH_MUSIC = false;
//#else
//#expand static final boolean PREFETCH_MUSIC = %PREFETCH_MUSIC%;
//#endif

//#if PREFETCH_SFX == ""
//#	static final boolean PREFETCH_SFX = false;
//#else
//#expand static final boolean PREFETCH_SFX = %PREFETCH_SFX%;
//#endif
//#endif

// region Map

	// layers enum
	static final int LAYER_GROUND = 0,
			LAYER_VENT = 1,
			LAYER_ROOF = 2,
			LAYER_UNDERGROUND = 3;

	// zones enum
	static final int ZONE_PLAYER_CELL = 1,
			ZONE_INMATE_CELL = 2,
			ZONE_ROLLCALL = 3,
			ZONE_CANTEEN = 4,
			ZONE_SHOWER = 5,
			ZONE_GYM = 6,
			ZONE_LAUNDRY = 7,
			ZONE_KITCHEN = 8,
			ZONE_WOODSHOP = 9,
			ZONE_METALSHOP = 10,
			ZONE_TAILORSHOP = 11,
			ZONE_DELIVERIES = 12,
			ZONE_JANITOR = 13,
			ZONE_GARDENING = 14;

	// schedule enum
	static final int SC_LIGHTSOUT = 0,
			SC_MORNING_ROLLCALL = 1,
			SC_BREAKFAST = 2,
			SC_WORK_PERIOD = 3,
			SC_MIDDAY_ROLLCALL = 4,
			SC_AFTERNOON_FREETIME = 5,
			SC_EVENING_MEAL = 6,
			SC_EXCERCISE_PERIOD = 7,
			SC_SHOWER_BLOCK = 8,
			SC_EVENING_FREETIME = 9,
			SC_EVENING_ROLLCALL = 10,
			SC_LOCKDOWN = 11;

	// jobs enum
	static final int JOB_UNEMPLOYED = 0,
			JOB_LAUNDRY = 1,
			JOB_GARDENING = 2,
			JOB_JANITOR = 3,
			JOB_WOODSHOP = 4,
			JOB_METALSHOP = 5,
			JOB_KITCHEN = 6,
			JOB_DELIVERIES = 7,
			JOB_TAILOR = 8,
			JOB_MAILMAN = 9,
			JOB_LIBRARY = 10;

	static final int NOTE_WELCOME = 0,
			NOTE_JOB_LOST = 1,
			NOTE_SOLITARY = 2;

	static final int COUNT_JOBS = 11;

	static final int MAX_JOB_QUOTA = 20;
	static final int NPC_JOB_QUOTA_THRESHOLD = 10;
	static final int JOB_EXISTING_BIT = 1;
	static final int JOB_OCCUPIED_BIT = 2;

	static final byte COLL_BIT_SOLID_AI = 1 << 4;
	static final byte COLL_BIT_AVOID_AI = 1 << 5;
	static final byte COLL_BIT_CAST_SHADOW = 1 << 6;

	// collision types
	static final byte COLL_NONE = 0;
	static final byte COLL_NOT_SOLID_INTERACT = 1 | COLL_BIT_AVOID_AI;
	static final byte COLL_SOLID = 2 | COLL_BIT_SOLID_AI | COLL_BIT_CAST_SHADOW;
	static final byte COLL_SOLID_TRANSPARENT = 3 | COLL_BIT_SOLID_AI | COLL_BIT_CAST_SHADOW;
	static final byte COLL_SOLID_INTERACT = 4 | COLL_BIT_AVOID_AI;
	static final byte COLL_DOOR = 5 | COLL_BIT_CAST_SHADOW;
	static final byte COLL_DOOR_STAFF = 6 | COLL_BIT_CAST_SHADOW;
	static final byte COLL_DESK = 7 | COLL_BIT_AVOID_AI;
	static final byte COLL_TABLE = 8 | COLL_BIT_SOLID_AI;
	static final byte COLL_GYM = 9 | COLL_BIT_AVOID_AI;
	static final byte COLL_SHOWER = 10;
	static final byte COLL_DIGGED_WALL = 11;
	static final byte COLL_DETECTOR = 12;
	static final byte COLL_SOLID_NO_SHADOW = 13 | COLL_BIT_SOLID_AI;

// endregion Map

// region Sound

	static final int MUSIC_THEME = 0,
			MUSIC_LIGHTSOUT = 1,
			MUSIC_GENERIC = 2,
			MUSIC_CANTEEN = 3,
			MUSIC_ROLLCALL = 4,
			MUSIC_SHOWER = 5,
			MUSIC_WORK = 6,
			MUSIC_WORKOUT = 7,
			MUSIC_LOCKDOWN = 8,
			MUSIC_ESCAPED = 9;
	static final int COUNT_MUSIC = 10;

	static final int SFX_ACCOLADE = 0,
			SFX_BELL = 1,
			SFX_BUY = 2,
			SFX_CLOSE = 3,
			SFX_DOOR = 4,
			SFX_ENHIT = 5,
			SFX_OPEN = 6,
			SFX_PICKUP = 7,
			SFX_PLIP = 8,
			SFX_RUMBLE = 9,
			SFX_LOSE = 10;
	static final int COUNT_EFFECTS = 11;

// endregion Sound

// region Font

	static final int FONT_CACHE_SIZE = 64;
	static final int FONTS_COUNT = 2;

	static final int FONT_REGULAR = 0;
	static final int FONT_BOLD = 1;

	// font colors enum
	static final int FONT_COLOR_WHITE = 0, // 0xFFFFFF
			FONT_COLOR_BLACK = 1, // 0x000000
			FONT_COLOR_RED = 2, // 0x0000FF
			FONT_COLOR_GREY_C0 = 3, // 0xC0C0C0
			FONT_COLOR_GREY_B4 = 4, // 0xB4B4B4
			FONT_COLOR_GREY_7F = 5, // 0x7F7F7F
			FONT_COLOR_GREY_69 = 6, // 0x696969
			FONT_COLOR_GREY_23 = 7, // 0x232323
			FONT_COLOR_ORANGE = 8, // 0xFF8000
			FONT_COLOR_BLUE = 9, // 0x7BA7FF
			FONT_COLOR_LIGHTBLUE = 10, // 0x9BC4F3
			FONT_COLOR_YELLOW = 11, // 0xFFFF00
			FONT_COLOR_DARKBLUE = 12; // 0x003D80

	static final String FONT_REGULAR_RES = "/fontr";
	static final String FONT_BOLD_RES = "/fontb";

// endregion Font

}
