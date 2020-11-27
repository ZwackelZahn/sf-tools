import sys
import json
import hashlib

from zipfile import ZipFile, ZIP_DEFLATED

inputFile = sys.argv[1]
outputFile = inputFile + ".ca"

with open(inputFile, 'r', encoding = "utf-8") as jsonFile:
    data = json.load(jsonFile)
    with ZipFile(outputFile, mode = 'w', compression = ZIP_DEFLATED) as archiveFile:
        for f in data:
            tag = hashlib.md5()
            tag.update(str(f["timestamp"]).encode("utf-8"))
            tag = tag.hexdigest()[:16]
            tagPlayers = tag + "/.players/"
            tagGroups = tag + "/.groups/"
            
            archiveFile.writestr(tag + "/.data", json.dumps({
              "timestamp": f["timestamp"],
              "offset": f.get("offset", 0),
              "version": f["version"]
            }, separators = (',', ':')))

            for p in f["players"]:
              archiveFile.writestr(tagPlayers + p["id"], json.dumps(p, separators = (',', ':')))
              
            for g in f["groups"]:
              archiveFile.writestr(tagGroups + g["id"], json.dumps(g, separators = (',', ':')))
            
            
