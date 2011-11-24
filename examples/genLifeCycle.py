#!/usr/bin/python

from generator import Sequence, Choice, Entry
from generator import generate
from generator import defaultActivityHook

def task2_hook(startTime, attrs):
    t0, t1, t2, t3 = defaultActivityHook(startTime, attrs)
    t1 += 60*60
    t2 += 60*60
    t3 += 60*60
    return t0, t1, t2, t3

process = Sequence([
	Entry("secondotask"),
	Entry("task1", task2_hook)
	])
generate(process, 1)
