
from sys import platform
import commands
import xml.etree.ElementTree as ET
import os
import sys
import shutil

if platform == "win32":
	from subprocess import check_output, call
	pwd = os.getcwd();  # change
	# elif platform == "linux" or platform == "linux2":
else:
	pwd = commands.getoutput('pwd')
# print "\npwd is " + pwd
if os.path.exists(pwd + "/Tejas-Simulator") == False:
	os.mkdir(pwd + "/Tejas-Simulator")
os.chdir(pwd + "/Tejas-Simulator")
pwd = pwd + "/Tejas-Simulator"


# Install from tar-ball or repository
print "\n\nStep 1 : cloning Tejas source"
print "\n\nTejas Installation Option : "
print "\t1) to install Tejas from the tar-ball in installation kit"

tejas_tar_ball = commands.getoutput('echo $TEJAS_TAR_BALL')
status = os.system("tar -xvf " + tejas_tar_ball)

if status!=0:
    print "error in extracting tejas : " + + str(os.WEXITSTATUS(status))
    print '\n'
    sys.exit(1)
else:
	print "extracted tejas successfully"


# PIN
print "\n\nStep 2 : setting up Pin"
pin_path = ""

# download PIN
# if platform == "linux" or platform == "linux2":
# download PIN
print "Download version 62732 from Intel's website : http://software.intel.com/en-us/articles/pintool-downloads"
print "Please read the license before downloading : http://software.intel.com/sites/landingpage/pintool/extlicense.txt"
print "NOTE : This installation script requires Pin in the form of a tar-ball"

pin_tar_path = commands.getoutput('echo $PINPATH')
if os.path.exists(pin_tar_path) == False:
    print "Pin not provided.. Exiting."
    sys.exit(1)



# extract PIN
print "extracting PIN"
if os.path.exists(pwd + "/PIN"):
	shutil.rmtree(pwd + "/PIN")
os.mkdir(pwd + "/PIN")
os.chdir(pwd + "/PIN")
status, output = commands.getstatusoutput("tar -xvf " + pin_tar_path)
if status != 0:
	print "error extracting Pin : " + str(os.WEXITSTATUS(status))
	print output
	sys.exit(1)
filenames = os.listdir(pwd + "/PIN")
pin_path = pwd + "/PIN/" + filenames[0]
os.chdir(pwd)


# configuring
print '\n\nStep 3 : Configuring'

jniInclude=""
fname=""
if platform == "darwin":
	jniInclude = "-I/System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers"
else:
	jniInclude =  "-I/usr/lib/jvm/java-6-openjdk-amd64/include -I/usr/lib/jvm/java-7-openjdk-amd64/include -I/usr/lib/jvm/java-8-openjdk-amd64/include -I/usr/lib/jvm/java-8-openjdk-amd64/include/linux -I/usr/lib/jvm/java-9-openjdk-amd64/include -I/usr/lib/jvm/java-6-openjdk-i386/include -I/usr/lib/jvm/java-7-openjdk-i386/include -I/usr/lib/jvm/java-8-openjdk-i386/include  -I/usr/lib/jvm/java-9-openjdk-i386/include"
fname = 'Tejas/src/emulator/pin/makefile_linux_mac'

print 'setting PINPATH in ' + fname + " to " + pin_path
f = open(fname, 'r')
lines = f.readlines()
i = 0
for line in lines:
	if "PIN_KIT ?=" in line:
		lines[i] = "PIN_KIT ?=" + pin_path + "\n"
	if "JNINCLUDE =" in line:
		lines[i] = "JNINCLUDE =" + jniInclude + "\n"
	i = i + 1
f.close()
f = open(fname, 'w')
for line in lines:
	f.write(line)
f.close()

# update config.xml
fname  = 'Tejas/src/simulator/config/config.xml'
tree = ET.parse(fname)
root = tree.getroot()
emulator = root.find('Emulator')
# if platform == "linux" or platform == "linux2":
print 'setting PinTool in ' + fname + ' to ' + pin_path
emulator.find('PinTool').text = pin_path
print 'setting PinInstrumentor in ' + fname + ' to ' + pwd + '/Tejas/src/emulator/pin/obj-pin/causalityTool.so'
emulator.find('PinInstrumentor').text = pwd + "/Tejas/src/emulator/pin/obj-pin/causalityTool.so"
print 'setting ShmLibDirectory in ' + fname + ' to ' + pwd + '/Tejas/src/emulator/pin/obj-comm'
emulator.find('ShmLibDirectory').text = pwd + "/Tejas/src/emulator/pin/obj-comm"
print 'setting KillEmulatorScript in ' + fname + ' to ' + pwd + '/Tejas/src/simulator/main/killAllDescendents.sh'
emulator.find('KillEmulatorScript').text = pwd + "/Tejas/src/simulator/main/killAllDescendents.sh"

system = root.find('System')
noc = system.find('NOC')
print 'setting NocConfigFile in ' + fname + ' to ' + pwd + '/Tejas/src/simulator/config/NocConfig.txt'
noc.find('NocConfigFile').text = pwd + '/Tejas/src/simulator/config/NocConfig.txt'


# Change NOC config path
# l2=root.find('L2')
# print 'setting NocConfigFile in ' + fname + ' to ' + pwd + '/Tejas/src/simulator/config/NocConfig.txt'
# l2.find('NocConfigFile').text = pwd + "/Tejas/src/simulator/config/NocConfig.txt"

if sys.version_info < (2, 7):
	tree.write(fname, encoding="UTF-8")
else:
	tree.write(fname, encoding="UTF-8", xml_declaration=True)

print "configure successful"




# building
print '\n\nStep 4 : Building'
os.chdir('Tejas')
# if platform == "linux" or platform == "linux2":
pwd = commands.getoutput('pwd')
print "pwd is " + pwd
status, output = commands.getstatusoutput("ant make-jar")
if status != 0 or os.path.exists(pwd + "/src/emulator/pin/obj-pin/causalityTool.so") == False or os.path.exists(pwd + "/src/emulator/pin/obj-comm/libshmlib.so") == False:
	print "error building : " + str(os.WEXITSTATUS(status))
	print output
	sys.exit(1)
else:
	print "build successful"



# # test-run : hello world
# # if platform == "linux" or platform == "linux2":
# print '\n\nStep 5 : Test Run - Hello World'
# os.chdir('..')
# pwd=commands.getoutput('pwd')
#
# if os.path.exists(pwd + '/outputs')==False:
# 	os.mkdir('outputs')
# if os.path.exists(pwd + '/tests')==False:
# 	os.mkdir('tests')
# 	if os.path.exists(pwd + '/tests/hello_world.cpp')==False:
# 		f = open(pwd + '/tests/hello_world.cpp', 'w')
# 		f.write("#include<iostream>\nint main() {\nstd::cout<<\"hello world\"<<std::endl;\nreturn (0);\n}\n")
# 		f.close()
#
# cmd = "g++ " + pwd + "/tests/hello_world.cpp -o " + pwd + "/tests/hello_world.o"
# print cmd
# status, output = commands.getstatusoutput(cmd)
# if status != 0 or os.path.exists(pwd + "/tests/hello_world.o") == False:
# 	print "error compiling test file : " + str(os.WEXITSTATUS(status))
# 	print output
# 	sys.exit(1)
#
# cmd = "java -jar " + pwd + "/Tejas/jars/tejas.jar " + pwd + "/Tejas/src/simulator/config/config.xml " + pwd + "/outputs/hello_world.output " + pwd + "/tests/hello_world.o"
# print cmd
# status = os.system(cmd)
#
#
# if status != 0 or os.path.exists(pwd + "/outputs/hello_world.output") == False:
# 	print "error running test : " + str(os.WEXITSTATUS(status))
# 	sys.exit(1)


# print "Helloworld test completed \n\n\n"
print "------------- Tejas installed successfuly ----------------\n"
print "Tejas jar has been created here : " + pwd + "/Tejas/jars/tejas.jar"
print "Configuration file is kept here : " + pwd + "/Tejas/src/simulator/config/config.xml"
print "Use this command to run tejas : java -jar <tejas.jar> <config-file> <output-file> <input-program and arguments>"
