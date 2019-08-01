import sys
import json

SF_OTHERPLAYER_CHECK = 'otherplayer'
SF_OWNPLAYER_CHECK = 'ownplayer'

SF_OTHERPLAYER_LOOKAT = 'otherplayer.playerlookat'
SF_OTHERPLAYER_NAME = 'otherplayername.r'
SF_OTHERPLAYER_UNITS = 'otherplayerunitlevel'
SF_OTHERPLAYER_ACHIEVEMENT = 'otherplayerachievement'
SF_OTHERPLAYER_FORTRESSRANK = 'otherplayerfortressrank'

SF_OWNPLAYER_LOOKAT = 'ownplayersave.playerSave'
SF_OWNPLAYER_UNITS = 'unitlevel'
SF_OWNPLAYER_ACHIEVEMENT = 'achievement'
SF_OWNPLAYER_NAME = 'ownplayername.r'
SF_OWNPLAYER_PETS = 'ownpets.petsSave'

SF_OWNGROUP = 'owngroupsave.groupSave'
SF_OWNGROUP_NAMES = 'owngroupmember.r'
SF_OWNGROUP_KNIGHTS = 'owngroupknights.r'
SF_OWNGROUP_POTIONS = 'owngrouppotion.r'

SF_MOUNTS = {
	0 : 0,
	1 : 10,
	2 : 20,
	3 : 30,
	4 : 50
}

SF_CLASSES = {
	1 : 'warrior',
	2 : 'mage',
	3 : 'scout',
	4 : 'assassin',
	5 : 'battle_mage',
	6 : 'berserker'
}

SF_RACES = {
	1 : 'human',
	2 : 'elf',
	3 : 'dwarf',
	4 : 'gnome',
	5 : 'orc',
	6 : 'dark_elf',
	7 : 'goblin',
	8 : 'demon'
}

SF_SEXES = {
	1 : 'male',
	2 : 'female'
}

SF_ROLES = {
	1 : 'owner',
	2 : 'officer',
	3 : 'member',
	4 : 'invited'
}

SF_POTIONS = {
	0 : 'none',
	1 : 'strength',
	2 : 'dexterity',
	3 : 'intelligence',
	4 : 'constitution',
	5 : 'luck',
	6 : 'strength',
	7 : 'dexterity',
	8 : 'intelligence',
	9 : 'constitution',
	10 : 'luck',
	11 : 'strength',
	12 : 'dexterity',
	13 : 'intelligence',
	14 : 'constitution',
	15 : 'luck',
	16 : 'life'
}

# Tags to be excluded from output
SF_OTHERPLAYER_SKIP = [
	'otherplayergroupname.r',
	'otherdescription.s',
	'otherplayerfriendstatus',
	'soldieradvice',
	'otherplayerpetbonus.petbonus',
	'success'
]

SF_OWNPLAYER_SKIP = [
	'messagelist.r',
	'Success',
	'attbonus',
	'combatloglist.s',
	'friendlist.r',
	'login count',
	'sessionid',
	'stoneperhournextlevel',
	'woodperhournextlevel',
	'inboxcapacity',
	'soulsperhournextlevel',
	'goldperhournextlevel',
	'underworldprice',
	'underworldupgradeprice',
	'underworldmaxsouls',
	'owndescription.s',
	'maxrank',
	'skipallow',
	'timestamp',
	'fortresspricereroll',
	'petsdefensetype',
	'singleportalenemylevel',
	'owngroupname.r',
	'owngrouprank',
	'witch',
	'owntower',
	'fortresschest',
	'fortressgroupprice',
	'unitprice',
	'owngroupdescription.s',
	'fortressGroupPrice',
	'upgradeprice',
	'fortressprice',
	'dragongoldbonus',
	'tavernspecial',
	'wagesperhour',
	'toilettfull',
	'maxupgradelevel',
	'serverversion',
	'cidstring',
	'tracking.s',
	'groupskillprice',
	'success',
	'cryptoid',
	'cryptokey',
	'fortresswalllevel',
	'chattime',
	'chathistory.s'
]

'''
	Helper class for item data
'''
class ItemData:
	def __init__(self, data):
		self.Socket = data[0]
		self.ID = data[1]
		self.Armor = data[2]
		self.Major = data[4]
		self.Minor = data[5]
		self.Least = data[6]
		self.Attribute1 = data[7]
		self.Attribute2 = data[8]
		self.Attribute3 = data[9]
		self.Gold = data[10] / 100

'''
	Helper class for weapon data
'''
class WeaponData:
	def __init__(self, data):
		self.Socket = data[0] # Corresponds to gem type, but changes between item types
		self.ID = data[1]
		self.DamageMin = data[2]
		self.DamageMax = data[3]
		self.Major = data[4]
		self.Minor = data[5]
		self.Least = data[6]
		self.Attribute1 = data[7]
		self.Attribute2 = data[8]
		self.Attribute3 = data[9]
		self.Gold = data[10] / 100
		
'''
	Helper class for other data
'''
class OtherData:
	def __init__(self, data):
		self.XP = data[3]
		self.XPRequired = data[4]
		self.PlayerHonor = data[5]
		self.PlayerRank = data[6]
		self.Race = SF_RACES[data[18] % 16]
		self.Sex = SF_SEXES[data[19] % 16]
		self.Class = SF_CLASSES[data[20] % 16]
		self.Strength = data[21] + data[26]
		self.Dexterity = data[22] + data[27]
		self.Intelligence = data[23] + data[28]
		self.Constitution = data[24] + data[29]
		self.Luck = data[25] + data[30]
		self.ItemHead = ItemData(data[39:51])
		self.ItemBody = ItemData(data[51:63])
		self.ItemHand = ItemData(data[63:75])
		self.ItemFoot = ItemData(data[75:87])
		self.ItemNeck = ItemData(data[87:99])
		self.ItemBelt = ItemData(data[99:111])
		self.ItemRing = ItemData(data[111:123])
		self.ItemMisc = ItemData(data[123:135])
		self.Weapon1 = WeaponData(data[135:147])
		self.Weapon2 = WeaponData(data[147:159])
		self.Mount = SF_MOUNTS[data[159] % 16]
		self.Book = ((data[163] - 10000) / 2160) # Has always 10000 added, unknown reason why
		self.Armor = data[168]
		self.Level = data[173]
		self.PotionType1 = data[194]
		self.PotionType2 = data[195]
		self.PotionType3 = data[196]
		self.PotionPower1 = data[200]
		self.PotionPower2 = data[201]
		self.PotionPower3 = data[202]
		self.FortressRank = None
		self.FortressUpgrades = data[247]
		self.FortressHonor = data[248]
		self.FortressKnights = data[258]

'''
	Helper class for own data
'''
class OwnData:
	def __init__(self, data):
		self.XP = data[8]
		self.XPRequired = data[9]
		self.PlayerHonor = data[10]
		self.PlayerRank = data[11]
		self.Race = SF_RACES[data[27] % 16]
		self.Sex = SF_SEXES[data[28] % 16]
		self.Class = SF_CLASSES[data[29] % 16]
		self.Strength = data[30] + data[35]
		self.Dexterity = data[31] + data[36]
		self.Intelligence = data[32] + data[37]
		self.Constitution = data[33] + data[38]
		self.Luck = data[34] + data[39]
		self.ItemHead = ItemData(data[48:60])
		self.ItemBody = ItemData(data[60:72])
		self.ItemHand = ItemData(data[72:84])
		self.ItemFoot = ItemData(data[84:96])
		self.ItemNeck = ItemData(data[96:108])
		self.ItemBelt = ItemData(data[108:120])
		self.ItemRing = ItemData(data[120:132])
		self.ItemMisc = ItemData(data[132:144])
		self.Weapon1 = WeaponData(data[144:156])
		self.Weapon2 = WeaponData(data[156:168])
		self.Mount = SF_MOUNTS[data[286] % 16] # Or 580
		self.Book = ((data[438] - 10000) / 2160) # Has always 10000 added, unknown reason why
		self.Armor = data[447]
		self.Level = data[465]
		self.PotionType1 = data[493]
		self.PotionType2 = data[494]
		self.PotionType3 = data[495]
		self.PotionPower1 = data[499]
		self.PotionPower2 = data[500]
		self.PotionPower3 = data[501]
		self.FortressRank = data[583]
		self.FortressUpgrades = data[581]
		self.FortressHonor = data[582]
		self.FortressKnights = data[598]
		
'''
	Converts data from array to dictionary
'''
def processData(d, out):
	out['xp'] = {
		'level' : d.Level,
		'current' : d.XP,
		'required' : d.XPRequired,
		'book' : d.Book
	}
	
	out['attributes'] = {
		'strength' : d.Strength,
		'dexterity' : d.Dexterity,
		'intelligence' : d.Intelligence,
		'constitution' : d.Constitution,
		'luck' : d.Luck,
		'armor' : d.Armor
	}

	out['character'] = {
		'race' : d.Race,
		'gender' : d.Sex,
		'class' : d.Class
	}
	
	out['ranks'] = {
		'playerRank' : d.PlayerRank,
		'playerHonor' : d.PlayerHonor,
		'fortressRank' : d.FortressRank,
		'fortressHonor' : d.FortressHonor
    }
	
	out['mount'] = d.Mount

	out['potions'] = [
		{
			'type' : SF_POTIONS[d.PotionType1],
			'power' : d.PotionPower1
		},
		{
			'type' : SF_POTIONS[d.PotionType2],
			'power' : d.PotionPower2
		},
		{
			'type' : SF_POTIONS[d.PotionType3],
			'power' : d.PotionPower3
		}
	]
	
	out['fortress'] = {
		'upgrades' : d.FortressUpgrades,
		'knights' : d.FortressKnights
	}

'''
	Converts key-value pairs into dicts
'''
def processOther(kv, out):
	key, value = kv.split(':')
	
	if any(sk in key for sk in SF_OTHERPLAYER_SKIP):
		return
	elif SF_OTHERPLAYER_LOOKAT in key:
		data = [int(v) for v in list(filter(None, value.split('/')))]
		processData(OtherData(data), out)
		out['data'] = data
	elif SF_OTHERPLAYER_NAME in key:
		out['name'] = value
	elif SF_OTHERPLAYER_UNITS in key:
		vals = [int(v) for v in value.split('/')]
		out['fortress']['units'] = {
			'wall' : vals[0],
			'warrior' : vals[1],
			'archer' : vals[2],
			'mage' : vals[3]
		}
	elif SF_OTHERPLAYER_FORTRESSRANK in key:
		out['ranks']['fortressRank'] = int(value)
	elif SF_OTHERPLAYER_ACHIEVEMENT in key:
		achievements = [int(v) for v in list(filter(None, value.split('/')))]
		out['achievements'] = {
			'owned' : sum(achievements[0:int(len(achievements)/2)]),
			'total' : len(achievements) / 2
		}
	
def processOwn(kv, out):
	key, value = kv.split(':', 1)
	
	if any(sk in key for sk in SF_OWNPLAYER_SKIP):
		return
	elif SF_OWNPLAYER_LOOKAT in key:
		data = [int(v) for v in list(filter(None, value.split('/')))]
		processData(OwnData(data), out)
		out['data'] = data
	elif SF_OWNPLAYER_NAME in key:
		out['name'] = value
	elif SF_OWNPLAYER_UNITS in key:
		vals = [int(v) for v in value.split('/')]
		out['fortress']['units'] = {
			'wall' : vals[0],
			'warrior' : vals[1],
			'archer' : vals[2],
			'mage' : vals[3]
		}
	elif SF_OWNPLAYER_ACHIEVEMENT in key:
		achievements = [int(v) for v in list(filter(None, value.split('/')))]
		out['achievements'] = {
			'owned' : sum(achievements[0:int(len(achievements)/2)]),
			'total' : len(achievements) / 2
		}
	elif SF_OWNGROUP in key:
		out['group'] = {}
		
		data = [int(v) for v in list(filter(None, value.split('/')))]
		count = data[3]

		out['group']['treasure'] = data[214:214 + count]
		out['group']['instructor'] = data[264:264 + count]
		out['group']['role'] = [SF_ROLES[r] for r in data[314:314 + count]]
		out['group']['pet'] = data[390:390 + count]
	elif SF_OWNGROUP_NAMES in key:
		out['group']['names'] = value.split(',')
		
'''
	Process data
'''
def process(kv, out):
	player = {}
	
	if kv.startswith(SF_OTHERPLAYER_CHECK):
		for val in kv.split('&'):
			if 'chathistory' in val:
				continue
			processOther(val, player)
		
		out.append(player)
	elif SF_OWNPLAYER_NAME in kv:
		for val in kv.split('&'):
			processOwn(val, player)
		out.append(player)
		
'''
	Extract only matching keys from HAR file
'''
def lookup(har, out, key):
	if isinstance(har, dict):
		for k, v in har.items():
			if k == key:
				out.append(v)
			else:
				lookup(v, out, key)
	elif isinstance(har, list):
		for v in har:
			lookup(v, out, key)

'''
	Copy if in dict
'''
def copyifn(key, src, dst):
	if key in src:
		dst[key] = src[key]
			
'''
	Join group data with player data
'''
def join(data):
	out = {}
	
	group = data[0]['group']
	
	for i, name in enumerate(group['names']):
		out[name] = {
			'group' : {
				'role' : group['role'][i],
				'treasure' : group['treasure'][i],
				'instructor' : group['instructor'][i],
				'pet' : group['pet'][i]
			}
		}
	
	for p in data:
		if p['name'] not in out:
			out[p['name']] = {}

		o = out[p['name']]
		
		copyifn('xp', p, o)
		copyifn('character', p, o)
		copyifn('mount', p, o)
		copyifn('potions', p, o)
		copyifn('achievements', p, o)
		copyifn('attributes', p, o)
		copyifn('fortress', p, o)
		copyifn('ranks', p, o)
		copyifn('data', p, o)
	
	return out
	
'''
	HAR -> JSON
'''
def convert(har):
	out = []
	outr = []
	
	lookup(har, outr, 'text')
	
	for e in outr:
		process(e, out)
		
	return join(out)
	
'''
	Main
'''
def main(path):
	with open(path, 'r', encoding = 'utf8') as json_input_file:
		with open(path.split('.')[0] + '.json', 'w', encoding = 'utf8') as json_output_file:
			json.dump(convert(json.load(json_input_file)), json_output_file)

if __name__ == '__main__':
	main(sys.argv[1])