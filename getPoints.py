import sys

def printLonLat(lon, lat):
	print("%.20f, %.20f" % (lon, lat))

def getMatrix(lon1, lat1, lon2, lat2, numRow, numCol):
	offsetLon = (lon2 - lon1) / (numRow + 1)
	offsetLat = (lat2 - lat1) / (numCol + 1)
	for i in range(numRow + 2):
		for j in range(numCol + 2):
			printLonLat(lon1 + i * offsetLon, lat1 + j * offsetLat)

def getLine(lon1, lat1, lon2, lat2, num):
	offsetLon = (lon2 - lon1) / (num + 1)
	offsetLat = (lat2 - lat1) / (num + 1)
	for i in range(num + 2):
		printLonLat(lon1 + i * offsetLon, lat1 + i * offsetLat)

def main():
	print "get matrix: input 1 + zuoshangjiaoLon + zuoshangjiaoLat + youxiajiaoLon + youxiajiaoLat + num"
	print "get line: input 2 + zuoshangjiaoLon + zuoshangjiaoLat + youxiajiaoLon + youxiajiaoLat + numRow + numCol"
	userInput = raw_input('Enter your parameters: ')
	userInput = userInput.split()
	if userInput[0] == "1":
		getMatrix(float(userInput[1]), float(userInput[2]), float(userInput[3]), float(userInput[4]), int(userInput[5]), int(userInput[6]))
	else:
		getLine(float(userInput[1]), float(userInput[2]), float(userInput[3]), float(userInput[4]), int(userInput[5]))

main()