from operator import pos
import numpy as np
import matplotlib.pyplot as plt
#import seaborn as sns
import random
import pickle 
import time,os

START = 0
reward_list=[]
END_STATE=-1


def print_it(*args):
    y = open("rl_data.log","a")
    for k in args:
        y.write(str(k)+" ")
        print(k,end= " ")
    y.write("\n")
    y.close()
    print("")
    

possible_states=[]
for i in range(0,200,20):
    possible_states.append(i)

for j in np.geomspace(200,1000, num=10, endpoint=True):
    possible_states.append(j)

Q_values={}
for i in possible_states:
    Q_values[i] = {}
    for a in range(1,7):
        Q_values[i][a] = 0  # Q value is a dict of dict

if(os.path.exists("q_table.pickle")):
    file = open("q_table.pickle","rb")
    Q_values = pickle.load(file)
    file.close()


def get_bandwidth():
    y=open('bandwidth_log.txt','r')
    s=y.read().split('/n')
    size,time=s[-1].split(',')
    return float(size)/float(time)


class State:
    def __init__(self,file_size=0):
        for i in possible_states:
            if (i>file_size):
                self.state=possible_states[possible_states.index(i)-1]
                return 
        self.state=possible_states[-1]

        
    def nxtPosition_reward(self,n):
        
        reward=get_bandwidth()
        return reward

        

class Agent:

    def __init__(self,exploration_rate = 0.7,exploration_decay = 0.9, decay_gamma=0.9):
        self.states = []  # record position and action taken at the position
        self.actions = [i for i in range(1,7)]  #setting the possible number of threads
        self.State = State()
        self.lr = 0.2
        self.exp_rate = exploration_rate
        self.decay_gamma = decay_gamma
        self.exploration_decay = exploration_decay
        self.previous_action_state = None

        
    def chooseAction(self):
        global Q_values
        mx_nxt_reward = 0
        action = 1

        if np.random.uniform(0, 1) <= self.exp_rate:
            action = np.random.choice(self.actions)
            print_it("random action")
        else:
            #greedy action
            print_it("greedy action")
            for a in self.actions:
                current_position = self.State.state
                nxt_reward = Q_values[current_position][a]
                if nxt_reward >= mx_nxt_reward:
                    action = a
                    mx_nxt_reward = nxt_reward
        return action

    def takeAction(self, action):
        # update State
        return self.State.nxtPosition_reward(action)

    def reset(self):
        self.states = []
        self.State = State()
        reward=0
        
    def start(self,itera=10):
        global Q_values
        _=0
        reward=0
        while(_<itera):    
            print("exploration_rate",self.exploration_rate)                          
            if(self.State.state!=END_STATE):
                action = self.chooseAction()
                # append trace
                self.states.append([(self.State.state), action])
                # by taking the action, it reaches the next state
                reward += self.takeAction(action)
                print_it("Getting reward"+str(reward))
                print_it(self.states)
                self.State.state=END_STATE      
            else:
                reward_list.append(round(reward, 2))
                print_it("Game End Reward", reward)
                for s in reversed(self.states):
                    current_q_value = Q_values[s[0]][s[1]]
                    reward = current_q_value + self.lr * (self.decay_gamma * reward - current_q_value)
                    Q_values[s[0]][s[1]] = round(reward, 3)
                self.reset()
                _ += 1
                self.exp_rate=self.exp_rate*10/11
                file = open("q_table.pickle","wb")
                pickle.dump(Q_values,file)
                file.close()


    def predict(self,file_size):
        global Q_values
        reward=0
        self.State = State(file_size)
        action = self.chooseAction()
        self.states.append([(self.State.state), action])
        self.previous_action_state = [self.State.state,action]
        self.State.state=END_STATE
        return action
    
    def learn(self,bandwidth):
        print_it('updating q value for state action ',self.previous_action_state)
        reward=bandwidth
        s = self.previous_action_state
        reward_list.append(round(reward, 2))
        print_it("Game End Reward", reward)
        current_q_value = Q_values[s[0]][s[1]]
        reward = current_q_value + self.lr * (self.decay_gamma * reward - current_q_value)
        Q_values[s[0]][s[1]] = round(reward, 3)
        self.reset()
        self.exp_rate=self.exp_rate*self.exploration_decay
        file = open("q_table.pickle","wb")
        pickle.dump(Q_values,file)
        file.close()





if __name__ == "__main__":
    ag = Agent()
    #ag.start(1)
    print_it('The threads predicted for 100mb are ',ag.predict(100))
    print_it('If we get a bandwidth of 10mb/s, the value updation is ')
    ag.learn(10)
    