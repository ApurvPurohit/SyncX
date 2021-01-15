import os,time
import socket		
import threading
import json

import sys




from tkinter import *
from tkinter import ttk
from tkinter import filedialog

from tkinter import messagebox

import qlearning


rl = qlearning.Agent(0.2)



BUFFER_SIZE = 102400
path_of_saving_files=""
HOSTING_IP = ""
HOSTING_PORT = 8080


debug = False


NUMBER_OF_SYNCHRONOUS_THREADS = 12

REQUEST = "o"
RESPONSE = "r"
PERMANENT = "p"
TEMPORARY = "t"
NUMBER_OF_SOCKETS = 4
PACKET_FROM_SERVER= "s"
PACKET_FROM_CLIENT = "c"




is_connected= False

id_to_file_to_be_wrote = {}

id_to_file_to_be_read = {} 


name_to_path = {}


id_count = 0




recieving_threads= {}

sending_threads= {}



def get_optimal_sockets(file_size):
	value = rl.predict(file_size/1000000)

	print(value)
	# return min(int(file_size/25000000)+1,8) 
	return int(value)



def reset_all():
	
	global id_to_file_to_be_wrote, id_to_file_to_be_read, name_to_path,id_count

	id_to_file_to_be_wrote = {}

	id_to_file_to_be_read = {} 


	name_to_path = {}


	id_count = 0
	print(" disconnected ")




def printit(*values):
	if(debug==True):
		for u in values:
			printit(u,end = " ")
		printit("")


	# printit('Got connection from', addr) 


	# client.send('Thank you for connecting') 


	# client.close() 


def reliable_recv(client,size):
	global is_connected
	try:
		u = client.recv(size)

		while(not len(u)==size):
			recv_bytes = client.recv(size-len(u))
			if(len(recv_bytes)==0):
				printit("socket closed")
				return
			u = u + recv_bytes

		printit("recieved :",u)
		return u
	except Exception:
		printit(" disconnected")
		is_connected = False

# def reliable_send(client,data):
# 	sent_bytes = client.send(data)
# 	while(len(data)!=sent_bytes):
# 		sent_bytes = sent_bytes+ client.send(data[sent_bytes:])




def send_file(name,client,number_of_socket=NUMBER_OF_SOCKETS):
	file_size  = os.path.getsize(name_to_path[name])

	name_size = len(name)



	client.sendall(bytes(REQUEST,"utf-8"))
	#int
	client.sendall(name_size.to_bytes(4, byteorder='big'))
	#str
	client.sendall(bytes(name,"utf-8"))

	#long long
	client.sendall(file_size.to_bytes(8, byteorder='big'))
	#id
	client.sendall(int(5).to_bytes(4, byteorder='big'))
	#number of sockets
	number_of_socket = get_optimal_sockets(file_size)
	client.sendall((number_of_socket).to_bytes(4, byteorder='big'))




# Function for opening the  
# file explorer window 
def browseFiles(): 
	filez = filedialog.askopenfilenames(parent=window,title='Choose a file')
	paths = window.tk.splitlist(filez)
	printit(paths)
	for u in paths:
		name = u.split("/")[-1]
		name_to_path[name]=u


		while (threading.active_count()>NUMBER_OF_SYNCHRONOUS_THREADS):
			printit(threading.active_count())


		send_file(name, permanent_socket,NUMBER_OF_SOCKETS)
		time.sleep(0.09)


	printit(type(paths))
       

window=None



def exit_function():
	# Put any cleanup here.  
	global window
	window.quit()
	printit("quttting")
	window.destroy()
	window = None
	printit("done quiting")
	permanent_socket.close()





ip_config,saving_path_config,buffer_size_config = None, None, None

def do_changes():

	dic = {
    "saving_path": saving_path_config,
    "hosting_ip": ip_config,
    "buffer_size": buffer_size_config
		}

	with open('configuration.json', 'w') as outfile:
		json.dump(dic, outfile)


label_4 = None
entry_1 = None
entry_3 = None
configuration_window=None

def getFolderPath():
	global saving_path_config
	folder_selected = filedialog.askdirectory()
	label_4.config(text = folder_selected)
	saving_path_config = folder_selected
	# label_4.labelText = folderPath
    # label_4.depositLabel.config(text=folder_selected)

    


def submit():
	global ip_config, buffer_size_config,configuration_window
	try:
		ip_config = entry_1.get()
		buffer_size_config = int(entry_3.get())
		do_changes()
		configuration_window.destroy()
		configuration_window = None
		printit(saving_path_config,ip_config,buffer_size_config)
	except Exception as e :
		printit(e)
		messagebox.showerror("Error", "Please fill the boxes correctly")




def configuration():
	global label_4,entry_3,entry_1,configuration_window
	configuration_window = Tk()

	configuration_window.geometry("500x500")
	configuration_window.title('Configuration Master')
	label_0 =Label(configuration_window,text="Configuration master", width=20,font=("bold",20))
	label_0.place(x=90,y=60)
	label_1 =Label(configuration_window,text="Ip address", width=20,font=("bold",10))
	label_1.place(x=80,y=130)
	entry_1=Entry(configuration_window)
	entry_1.place(x=240,y=130)
	label_3 =Label(configuration_window,text="Buffer Size", width=20,font=("bold",10))
	label_3.place(x=68,y=180)
	entry_3=Entry(configuration_window)
	entry_3.place(x=240,y=180)
	label_4 =Label(configuration_window,text="Path to save files", width=20,font=("bold",10))
	label_4.place(x=70,y=230)
	var=IntVar()
	btnFind = ttk.Button(configuration_window, text="Browse Folder",command=getFolderPath)
	btnFind.place(x=235,y=230)
	Button(configuration_window, text='Done' , width=20,bg="black",fg='white',command=submit).place(x=180,y=380)
	configuration_window.mainloop()




while(True):

	try:

		f = open("configuration.json","r")
		dic = json.load(f)





		BUFFER_SIZE = int(dic["buffer_size"])
		path_of_saving_files = dic["saving_path"]
		HOSTING_IP = dic["hosting_ip"]
		HOSTING_PORT = 8080

		f.close()
		break
	except Exception:
		#change here python3 to python if on windows 
		# os.system("python3 configuration_selector.py")
		configuration()






s = socket.socket()		 
printit( "Socket successfully created")
s.bind((HOSTING_IP, HOSTING_PORT))		 
printit( "socket binded to %s" %(HOSTING_PORT) )
s.listen(20)	 


printit(HOSTING_IP)
permanent_socket = None


print("\n type {} in the ip field in android app and click connect \n make sure pc and mobile are on same network\n".format(HOSTING_IP))


def open_saved_files():
	os.startfile(path_of_saving_files)

def handle_gui():
	global window	                                                                                                 
	printit("starting handle_gui ")
	window = Tk() 
	window.protocol('WM_DELETE_WINDOW', exit_function)
	window.title('File Explorer') 
	window.geometry("420x300") 
	window.config(background = "white")
	label_file_explorer = Label(window,  
	                            text = "To send files click Send Files, to close click one exit or close ", 
	                            width = 60, height = 4,  
	                            fg = "blue") 
	button_explore = Button(window,  
	                        text = "Send Files", 
	                        command = browseFiles)  
	
	button_open = Button(window,  
	                        text = "open saved files", 
	                        command = open_saved_files)  
	   
	label_file_explorer.grid(column = 1, row = 1) 
	button_explore.grid(column = 1, row = 3) 
	button_open.grid(column = 1, row = 5)
	window.mainloop()



def get_file_handler_to_write(data_id):


	name = os.path.join(path_of_saving_files,id_to_file_to_be_wrote[data_id])
	if(not os.path.exists(name)):

		y = open(name,"ab+")
		printit("opening\n\n\n\n\n\n\n\n\n.....")
		y.close()

	printit("opening {}\n\n\n\n\n\n........".format(name))
	y= open(name, "rb+")
	return y



def handle_packet_recieving(client):

	printit("Entered_handle_data_recieving")

	#recive header
	data_id = reliable_recv(client,4)
	starting_point = reliable_recv(client,8)
	file_size = reliable_recv(client, 8)




	#decode them
	data_id  = int.from_bytes(data_id, byteorder='big')
	file = get_file_handler_to_write(data_id)
	starting_point = int.from_bytes(starting_point, byteorder='big')
	printit("stating point ",starting_point)
	file_size = int.from_bytes(file_size,byteorder='big')
	##change
	threading.current_thread().name = data_id
	if(data_id not in recieving_threads.keys()):
		recieving_threads[data_id] = [time.time(),1]
	else:
		recieving_threads[data_id][1] = recieving_threads[data_id][1]+1
	
	##change over

	file.seek(starting_point)
	bytes_recived = 0

	start = time.time()
	while(bytes_recived<file_size):

		# printit(bytes_recived,file_size)
		data_bytes = client.recv(BUFFER_SIZE)
		bytes_recived = bytes_recived +len(data_bytes)
		file.write(data_bytes)

	file.close()
	end = time.time()
	# printit("closing {}\n\n\n\n\n\n........".format(data_id))

	client.close()

	#change 
	recieving_threads[data_id][1] = recieving_threads[data_id][1]-1
	if(recieving_threads[data_id][1]==0):
		fi = open("records.txt","a")

		bandwidth = ((os.stat(file.name).st_size)/1000000)/(time.time()-recieving_threads[data_id][0])
		rl.learn(bandwidth)
		st = str("time_taken for recieving {}mb is {}".format((os.stat(file.name).st_size)/1000000,time.time()-recieving_threads[data_id][0]))


		fi.write(st+"\n")
		print(st,bandwidth)
		fi.close()


	#change over


	printit("time take is : ", end - start)
	printit("Exited_handle_data_recieving")

def handle_packet_sending(client):

	printit("Entered_handle_packet_sending")

	#reciver header
	data_id_bytes = reliable_recv(client,4)
	starting_point_bytes = reliable_recv(client,8)
	file_size_bytes = reliable_recv(client, 8)



	printit("recieved id")

	data_id = int.from_bytes(data_id_bytes, byteorder='big')
	starting_point = int.from_bytes(starting_point_bytes, byteorder='big')
	file_size = int.from_bytes(file_size_bytes,byteorder='big')


	##change
	threading.current_thread().name = data_id
	if(data_id not in sending_threads.keys()):
		sending_threads[data_id] = [time.time(),1]
	else:
		sending_threads[data_id][1] = sending_threads[data_id][1]+1
	
	##change over

	printit(id_to_file_to_be_read)

	

	while True:
		try:
			name = id_to_file_to_be_read[data_id]
			break
		except KeyError:
			time.sleep(0.01)
			printit("wainting for key")

	path_of_file = name_to_path[name]
	file = open(path_of_file,"rb+")
	file.seek(starting_point)


	#send header
	client.sendall(data_id_bytes)
	client.sendall(starting_point_bytes)
	client.sendall(file_size_bytes)


	start_time = time.time()
	#send file
	bytes_sent = 0
	while(bytes_sent!=file_size):
		sending_size = BUFFER_SIZE
		if(sending_size>file_size-bytes_sent):
			sending_size = file_size-bytes_sent
		read = file.read(sending_size)
		client.sendall(read)
		bytes_sent = bytes_sent +len(read)

	end_time = time.time()


	printit("time_taken",end_time-start_time)		

	file.close()
	#change 
	sending_threads[data_id][1] = sending_threads[data_id][1]-1
	if(sending_threads[data_id][1]==0):
		fi = open("records.txt","a")
		bandwidth = ((os.stat(file.name).st_size)/1000000)/(time.time()-sending_threads[data_id][0])

		rl.learn(bandwidth)

		st = str("time_taken for sending {}mb is {}".format((os.stat(file.name).st_size)/1000000,time.time()-sending_threads[data_id][0]))
		fi.write(st+"\n")
		print(st,bandwidth)
		fi.close()

		


	#change over


	client.close()


	printit("Exited_handle_packet_sending")



def handle_temporary_client(client):

	printit("Entered_handle_temporary_client")


	r = reliable_recv(client,1)

	r= r.decode("utf-8")
	if(r==PACKET_FROM_CLIENT):
		threading.Thread(target=handle_packet_recieving,name="handle_packet_recieving", args=(client,)).start()

	elif(r==PACKET_FROM_SERVER):
		threading.Thread(target=handle_packet_sending,name="handle_paket_sending", args=(client,)).start()

	else:
		printit("unkonwn type of non permanent connection \n closing it")
		client.close()

	printit("Exited_handle_temporary_client")







def handle_response(client):

	printit("Entered_handle_response")

	


	global id_to_file_to_be_read
	name_size_bytes = reliable_recv(client,4)
	name_size= int.from_bytes(name_size_bytes, byteorder='big')
	name_bytes = reliable_recv(client,name_size)
	name = name_bytes.decode("utf-8")


	file_size_bytes = reliable_recv(client,8)
	data_id_bytes = reliable_recv(client,4)
	socket_numbers_bytes = reliable_recv(client,4)

	file_size= int.from_bytes(file_size_bytes, byteorder='big')
	data_id = int.from_bytes(data_id_bytes, byteorder='big')
	number_of_sockets = int.from_bytes(socket_numbers_bytes, byteorder='big')

	id_to_file_to_be_read[data_id] = name


	# time.sleep(0.01)
	#to acknowledge the phone and all data have been registered
	# client.sendall(data_id_bytes)

	printit("Exited_handle_response")





def handle_request(client,number_of_socket=NUMBER_OF_SOCKETS):



	printit("Entered_handle_request")

	time.sleep(0.09)
	while (threading.active_count()>NUMBER_OF_SYNCHRONOUS_THREADS):
		printit(threading.active_count())

			

	global id_to_file_to_be_wrote,id_count
	name_size_bytes = reliable_recv(client,4)
	name_size= int.from_bytes(name_size_bytes, byteorder='big')
	name_bytes = reliable_recv(client,name_size)
	name = name_bytes.decode("utf-8")
	while (os.path.exists(name)):
		lis = name.split(".")
		name = lis[0]+"1." + ".".join(lis[1:])

	id_to_file_to_be_wrote[id_count] = name
	id_count= id_count+1

	file_size_bytes = reliable_recv(client,8)
	data_id_bytes = reliable_recv(client,4)
	socket_numbers_bytes = reliable_recv(client,4)

	file_size = int.from_bytes(file_size_bytes, byteorder='big')

	printit("request came",name,file_size_bytes)
	#send the pakket
	#char
	client.sendall(bytes(RESPONSE,"utf-8"))
	#int
	client.sendall(name_size_bytes)
	#str
	client.sendall(name_bytes)

	#long long
	client.sendall(file_size_bytes)
	#id
	client.sendall((id_count-1).to_bytes(4, byteorder='big'))
	#number of sockets
	number_of_socket = get_optimal_sockets(file_size) 
	client.sendall((number_of_socket).to_bytes(4, byteorder='big'))

	printit("Exited_handle_request")



def quit(r):
	r.destroy()
	printit("distroyed")


def start_permanent_reciver(client):
	global window,is_connected
	is_connected = True
	print(" connected")


	printit("Entered_permanet_reciver")



	try:


	
		

		u = reliable_recv(client,1)
		while(u):



			type_of_message = u.decode("utf-8")
			if(type_of_message==REQUEST):
				handle_request(client)			
			elif(type_of_message==RESPONSE):
				handle_response(client)			
				
			else:

				printit("unknown type of message : ",type_of_message)

			u = reliable_recv(client,1)

	except ConnectionResetError:
		client.close()

	is_connected = False
	printit("  disconnected from permatnet" )
	printit("Exited_permanet_reciver")
	reset_all()
	try:
		if(window!=None):
			printit("window not none")
			window.quit()		
			window.destroy()
	except Exception:
		pass


	# window = None
	printit("the end")
	



#first message to this funciton 
def handle_connection(client):
	printit("Entered handle_connection")


	global permanent_socket


	u = reliable_recv(client,1)
	type_of_client = u.decode("utf-8")	

	if(type_of_client==PERMANENT):
		permanent_socket = client
		threading.Thread(target=start_permanent_reciver,name="permanent", args=(client,)).start()
		threading.Thread(target=handle_gui,name="gui").start()

		

	elif(type_of_client==TEMPORARY):
		threading.Thread(target=handle_temporary_client,name="temporary", args=(client,)).start()
	else:
		printit("unkonwn type of connection disconnecting")
		client.close()


	printit("Exited handle_connection")




def typing():
	while True:
		y = input()
		if(y=="q"):
			printit("starting exit")
			os._exit(1)
			print("Thanks for using")
		elif(y=="o"):
			os.startfile(path_of_saving_files)
		for thread in threading.enumerate(): 
			printit(thread.name)
			# if thread.name == "permanent":
			# 	print(dir(thread))


print(" type connect on android app to connect")
print(" 1. type q to quit\n 2. type o to open saving folder\n 3. to change configuration change values in configuration.json file")
threading.Thread(target=typing,name="typing").start()

while True: 

	printit("while loop ")

	client, addr = s.accept()	 
	printit("accepted")
	
	printit(threading.active_count())
			
	handle_connection(client)
